package com.provoly.replay;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetService;
import com.provoly.clients.ModelService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.kafka.KafkaTools;
import com.provoly.replay.entity.ErrorReport;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ErrorService {

    private final Logger log;
    private final DatasetService datasetService;
    private final ModelService modelService;
    private final KafkaProducer producer;
    private final KafkaTools kafkaTools;

    public static final String SUMMARY_TOPIC = "summary-error";
    public static final String STORE_NAME = "aggregate-store";
    public static final String STREAM_ID = "stream";
    public static final String RESET = "reset";

    public Map<String, ErrorReport> errorReports = new HashMap<>();

    public ErrorService(Logger log, @RestClient DatasetService datasetService, @RestClient ModelService modelService,
            KafkaProducer producer, KafkaTools kafkaTools) {

        this.log = log;
        this.datasetService = datasetService;
        this.modelService = modelService;
        this.producer = producer;
        this.kafkaTools = kafkaTools;
    }

    @Incoming("summary")
    public CompletionStage<Void> consume(IncomingKafkaRecord<String, ErrorReport> record) {
        errorReports.put(record.getPayload().getTopic(), record.getPayload());
        return record.ack();
    }

    public Collection<ErrorReport> getReports() {
        for (var record : errorReports.values()) {
            if (record.getoClass() == null) {
                String datasetName = record.getTopic().replace("dlq-", "");
                String oClassId = datasetName;
                try {
                    oClassId = datasetService.getDatasetVersionByDatasetName(datasetName).getoClass().toString();
                } catch (BusinessException e) {
                    log.errorv("Could not find dataset for dlq {0}", record.getTopic());
                }
                record.setoClass(oClassId);
            }
        }
        return errorReports.values();
    }

    public void acquitRecords(UUID id) {
        var oClass = modelService.get(id);
        var topicErrorName = kafkaTools.getTopicErrorName(oClass.getName());
        log.infof("seek to end dlq topic %s", topicErrorName);
        try (var kafkaConsumer = kafkaTools.getConsumer(String.class)) {
            kafkaConsumer.subscribe(List.of(topicErrorName), new DlqRebalancedConsumer(kafkaConsumer));
            kafkaConsumer.poll(Duration.of(10, ChronoUnit.SECONDS));
        }
        updateErrorReport(topicErrorName);
    }

    public void replayRecord(Set<UUID> classIds) {
        try (var kafka = kafkaTools.getConsumer(String.class)) {
            for (var classId : classIds) {
                replay(kafka, classId);
            }
        }
    }

    private List<TopicPartition> getTopicPartitions(String topicErrorName, List<PartitionInfo> partitionInfos) {
        List<TopicPartition> partitions = new ArrayList<>();
        for (var partitionInfo : partitionInfos) {
            partitions.add(new TopicPartition(topicErrorName, partitionInfo.partition()));
        }
        return partitions;
    }

    private void replay(KafkaConsumer<String, String> kafka, UUID classId) {
        var oClass = modelService.get(classId);
        var topicErrorName = kafkaTools.getTopicErrorName(oClass.getName());
        var topicItemName = kafkaTools.getTopicItemName(oClass.getName());

        var partitions = getTopicPartitions(topicErrorName, kafka.partitionsFor(topicErrorName));
        var endOffset = kafka.endOffsets(partitions);

        log.infof("Replay for class %s:%s from topic %s->%s", oClass.getId(), oClass.getName(), topicErrorName, topicItemName);
        kafka.subscribe(List.of(topicErrorName));
        int globalCount = 0;

        while (!endOffset.isEmpty()) {
            log.infov("actual end offset :{0}", endOffset);

            var rcv = kafka.poll(Duration.of(10, ChronoUnit.SECONDS));
            if (rcv.isEmpty())
                break;

            log.tracef("Polling items for class %s", classId);
            for (var item : rcv) {
                if (item.headers().lastHeader(RESET) != null)
                    continue;
                var topicPartition = isAtTheEnd(item.offset(), endOffset);
                if (topicPartition != null) {
                    endOffset.remove(topicPartition);
                }
                var record = new ProducerRecord<>(topicItemName, item.key(), item.value());
                producer.send(record);
            }
            producer.flush();
            log.infof("Replay of %d items done", rcv.count());
            globalCount = globalCount + rcv.count();
        }

        log.infof("Global replay of %d items in topic %s", globalCount, topicItemName);
        updateErrorReport(topicErrorName);
    }

    private TopicPartition isAtTheEnd(Long itemPosition, Map<TopicPartition, Long> endOffsets) {
        for (var entry : endOffsets.entrySet()) {
            if (entry.getValue().equals(itemPosition)) {
                log.infov("Reach the end of topic partition {0} : {1}", entry.getKey(), entry.getValue());
                return entry.getKey();
            }
        }
        return null;
    }

    private void updateErrorReport(String topicErrorName) {
        log.infov("Update error reports for topic : {0}", topicErrorName);

        var props = kafkaTools.getProducerProperties();
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", StringSerializer.class);

        try (Producer<String, String> producerError = new KafkaProducer<>(props)) {
            var rec = new ProducerRecord<>(topicErrorName, UUID.randomUUID().toString(), RESET);
            rec.headers().add(RESET, "true".getBytes(StandardCharsets.UTF_8));
            producerError.send(rec);
            producerError.flush();
        }
    }

}
