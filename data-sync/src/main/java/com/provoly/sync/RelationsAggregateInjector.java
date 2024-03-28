package com.provoly.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.DataVirt;
import com.provoly.common.relation.RelationsAggregateDto;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecordBatch;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RelationsAggregateInjector {

    private static final int MAX_RELATIONS_BULK_SIZE = 3000;

    @Inject
    Logger log;

    @Inject
    @RestClient
    DataVirt dataVirt;

    /**
     * Received aggregate by batch of 100 aggregate
     * For each aggregate, we split it in bulk of about MAX_RELATION_BULK_SIZE relations and send it to data-virt
     *
     * @param records
     * @return
     */
    @Incoming("relation-aggregate")
    @Blocking
    public CompletionStage<Void> consumeRelationAggregate(KafkaRecordBatch<String, RelationsAggregateDto> records) {
        try {
            log.infof("Consuming %d aggregates relation for a total of %d relations", records.getRecords().size(),
                    countRelations(records));
            // We using a map to deduplicate by aggregateId
            var recordsMap = new LinkedHashMap<String, KafkaRecord<String, RelationsAggregateDto>>();
            for (var record : records) {
                var previous = recordsMap.put(record.getPayload().aggregateId, record);
                if (previous != null) {
                    var previousAggregate = previous.getPayload();
                    log.debugf("Saving %d relations for %s", previousAggregate.size(), previousAggregate.aggregateId);
                }
            }

            var buklkRelations = new ArrayList<RelationsAggregateDto>();
            var bulkSize = 0;
            for (var record : recordsMap.values()) {
                bulkSize += record.getPayload().size();
                buklkRelations.add(record.getPayload());

                if (bulkSize > MAX_RELATIONS_BULK_SIZE) {
                    dataVirt.updateAggregate(buklkRelations);
                    bulkSize = 0;
                    buklkRelations.clear();
                }
            }
            dataVirt.updateAggregate(buklkRelations);
            log.tracef("Relations aggregates sent");
            return records.ack();
        } catch (Exception e) {
            return records.nack(e);
        }

    }

    @Incoming("dlq-relation-aggregate")
    public CompletionStage<Void> dead(Message<String> rejected) {
        IncomingKafkaRecordMetadata<String, String> metadata = rejected.getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow(() -> new IllegalArgumentException("Expected a message coming from Kafka"));
        String reason = new String(metadata.getHeaders().lastHeader("dead-letter-reason").value());
        log.debugf("The message '%s' has been rejected and sent to the DLT. The reason is: '%s'.", rejected.getPayload(),
                reason);
        return rejected.ack();
    }

    private int countRelations(KafkaRecordBatch<String, RelationsAggregateDto> records) {
        return records.getRecords().stream().mapToInt(r -> r.getPayload().size()).sum();
    }
}
