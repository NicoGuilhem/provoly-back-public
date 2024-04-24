package com.provoly.sync;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecordBatch;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.vertx.core.json.JsonObject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import kafka.KafkaTools;

@ApplicationScoped
public class DsTopicConsumer {

    @Inject
    Logger log;

    @Inject
    Service service;

    @Inject
    KafkaProducer kafkaProducer;

    @Inject
    KafkaTools kafkaTools;

    @Incoming("item")
    @Blocking
    public CompletionStage<Void> consumeNew(KafkaRecordBatch<String, JsonObject> batch) {
        try {
            List<KafkaRecord<String, JsonObject>> records = batch.getRecords();
            var result = batch.getLatestOffsetRecords().values().stream()
                    .map(this::buildLogFrom)
                    .collect(Collectors.joining("|"));
            log.infof("Receiving a batch of size: %d => %s", records.size(), result);
            service.consume(records);
        } catch (Exception e) {
            log.warn("Fallback to one by one caused by :", e);
            oneByOne(batch);
        }
        return batch.ack();
    }

    public void oneByOne(KafkaRecordBatch<String, JsonObject> records) {
        for (KafkaRecord<String, JsonObject> record : records) {
            try {
                String msg = buildLogFrom(record);
                log.infof("Trying to handling a single record : %s", msg);
                service.consume(record);
            } catch (Exception e) {
                log.errorf(e, "Error when inserting objet %s/%s@%s", record.getTopic(), record.getPartition(),
                        record.getTimestamp());
                notifyError(record, e);
            }
        }
    }

    private String buildLogFrom(KafkaRecord<String, JsonObject> record) {
        var recordMeta = record.getMetadata(IncomingKafkaRecordMetadata.class).get();
        var msg = recordMeta.getTopic() + "@" + recordMeta.getPartition() + "[" + recordMeta.getOffset() + "]";
        return msg;
    }

    private void notifyError(KafkaRecord<String, JsonObject> record, Exception exception) {
        try {
            var topicErrorName = kafkaTools.buildTopicErrorName(record.getTopic());
            kafkaTools.createTopicIfNeeded(topicErrorName);
            var errorRecord = new ProducerRecord<>(topicErrorName, UUID.randomUUID().toString(),
                    record.getPayload().toString());
            errorRecord.headers().add("exception", exception.getMessage().getBytes(StandardCharsets.UTF_8));
            kafkaProducer.send(errorRecord);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Stopping data-sync", e);
        }
    }

}
