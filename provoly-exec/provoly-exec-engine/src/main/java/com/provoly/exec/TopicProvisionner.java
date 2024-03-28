package com.provoly.exec;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.ItemsService;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.kafka.KafkaTools;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.search.Direction;
import com.provoly.common.search.SortType;
import com.provoly.exec.model.DataSourceProviding;
import com.provoly.exec.model.DatasetOutcome;
import com.provoly.exec.model.JobExecution;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TopicProvisionner {

    @Inject
    KafkaTools kafka;

    @Inject
    @RestClient
    ItemsService itemsService;

    @Inject
    Logger log;

    public String createInTopic(JobExecution jobExecution, DataSourceProviding dataSourceProviding) {
        var topicName = buildInTopicName(jobExecution, dataSourceProviding);
        kafka.createTopics(Set.of(topicName));
        return topicName;
    }

    public void loadTopic(JobExecution jobExecution, DataSourceProviding dataSourceProviding) {
        String topicName = buildInTopicName(jobExecution, dataSourceProviding);
        log.infof("loading topic %s", topicName);

        itemsService
                .getPaginateItems(dataSourceProviding.getDataSourceId(), MetadataSystem.ID.getId(),
                        Direction.asc, SortType.METADATA)
                .subscribe()
                .with(result -> sendItemsToTopic(topicName, result));

    }

    private void sendItemsToTopic(String topicName, ItemsSearchResultDto itemSearchResult) {
        try (var producer = kafka.getProducer(ObjectMapperSerializer.class)) {
            itemSearchResult.items()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .map(item -> new ProducerRecord<>(topicName, item.getId(), item))
                    .forEach(producer::send);
        }
    }

    public String createOutTopic(JobExecution jobExecution, DatasetOutcome datasetOutcome) {
        var topicName = buildTopicName("out", jobExecution, datasetOutcome.getDatasetId());
        kafka.createTopics(Set.of(topicName));
        return topicName;
    }

    private String buildInTopicName(JobExecution jobExecution, DataSourceProviding dataSourceProviding) {
        return buildTopicName("in", jobExecution, dataSourceProviding.getDataSourceId());
    }

    private String buildTopicName(String way, JobExecution jobExecution, UUID dataId) {
        return "transfo-" + way + "-" + jobExecution.getId() + "__" + dataId;
    }

    private void handleError(Throwable t) {
        log.errorf(t, "An error occured while retreive items");
    }
}
