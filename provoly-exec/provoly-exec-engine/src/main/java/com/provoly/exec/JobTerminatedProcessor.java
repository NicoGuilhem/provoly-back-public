package com.provoly.exec;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.DatasetImportService;
import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.exec.ExecContextOutputDatasetInfo;
import com.provoly.common.exec.ExecEvent;
import com.provoly.common.imports.ImportParameter;
import com.provoly.common.item.ItemDto;
import com.provoly.exec.deserializer.ItemDtoDeserializer;
import com.provoly.exec.kafka.KafkaTools;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JobTerminatedProcessor {
    @Inject
    KafkaTools kafkaTools;

    @Inject
    Logger log;

    @Inject
    @RestClient
    DatasetImportService importService;

    public void importDataToNewDataset(ExecEvent execEvent) {
        log.infof("Retrieve transformed data from execution %s and import it", execEvent.jobExecutionId());
        KafkaConsumer<String, ItemDto> consumer = buildConsumer();

        for (Map.Entry<UUID, ExecContextOutputDatasetInfo> entry : execEvent.context().getOutTopicNameByDatasetId()
                .entrySet()) {
            String topicOutName = entry.getValue().topicName();
            log.infof("Get data from topic out : %s", topicOutName);

            UUID datasetId = entry.getKey();
            UUID newDataset = entry.getValue().datasetId();
            List<ItemDto> items = new ArrayList<>();
            var finished = false;

            consumer.subscribe(List.of(topicOutName));

            while (!finished) {
                var rcv = consumer.poll(Duration.of(5, ChronoUnit.SECONDS));
                log.infof("consumer poll size : %s", rcv.count());
                finished = rcv.count() == 0;

                for (var item : rcv) {
                    items.add(item.value());
                }
            }
            consumer.close();
            log.infof("Import %s items in new dataset version %s of dataset %s", items.size(), newDataset, datasetId);
            importService.importData(datasetId, newDataset, new ImportParameter(items, new DatasetVersionInformationDto(
                    "job-exec: %s".formatted(execEvent.jobExecutionId().toString()), Instant.now())));
        }
        log.infof("Import done for jobExecutionId %s", execEvent.jobExecutionId());
    }

    private KafkaConsumer<String, ItemDto> buildConsumer() {
        var prop = kafkaTools.getConsumerProperties();
        prop.putAll(Map.of(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ItemDtoDeserializer.class));

        return new KafkaConsumer<>(prop);
    }
}