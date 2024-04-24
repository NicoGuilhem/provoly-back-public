package com.provoly.replay;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.item.ItemDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.KafkaTools;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
public class ReplayTest {

    @Inject
    Logger log;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    ErrorController replayController;

    @Inject
    ObjectMapper mapper;

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    KafkaTools kafkaTools;

    private OClassWriteDto documentClass;
    private OClassWriteDto documentClass2;
    private OClassWriteDto documentClass3;

    @BeforeEach
    public void prepareData() {
        authService.authenticate();

        kafkaTools.getAdmin().deleteTopics(List.of("summary-error"));
        kafkaTools.createTopicIfNeeded("summary-error");
        kafkaTools.createTopicIfNeeded("data-replay-stream-aggregate-store-repartition");

        kafkaTools.createTopicIfNeeded("dlq-ds-document");
        kafkaTools.createTopicIfNeeded("dlq-ds-document2");
        kafkaTools.createTopicIfNeeded("dlq-ds-document3");

        kafkaTools.createTopicIfNeeded("ds-document");
        kafkaTools.createTopicIfNeeded("ds-document2");
        kafkaTools.createTopicIfNeeded("ds-document3");

        // create class vehicule
        var titleField = testData.createField("title", "string");
        var authorField = testData.createField("author", "string");
        var titleAttribute = testData.createAttribute("title", titleField);
        var authorAttribute = testData.createAttribute("author", authorField);
        var authorAttribute3 = testData.createAttribute("author3", authorField);

        documentClass = testData.createClassWithoutIdInName("document", titleAttribute);
        testData.createDataset("ds-document", documentClass.getId());

        documentClass2 = testData.createClassWithoutIdInName("document2", authorAttribute);
        testData.createDataset("ds-document2", documentClass2.getId());

        documentClass3 = testData.createClassWithoutIdInName("document3", authorAttribute3);
        testData.createDataset("ds-document3", documentClass3.getId());
    }

    @AfterEach
    public void cleaning() {
        testData.clean();
    }

    @Test
    public void replay_copyItemFromDlqToClassTopic() {
        String topicErrorName = kafkaTools.getTopicErrorName(documentClass.getName());
        String topicItemName = kafkaTools.getTopicItemName(documentClass.getName());

        try {
            // Write an item to the item error topic
            var item = new ItemDto(UUID.randomUUID(), "i1@id");
            String itemString = mapper.writeValueAsString(item);
            companion
                    .produce(String.class)
                    .fromRecords(new ProducerRecord<>(topicErrorName, UUID.randomUUID().toString(), itemString))
                    .awaitCompletion();

            // Call service to replay errors item
            replayController.replay(Set.of(documentClass.getId()));

            // Check item is added to item queue
            var received = companion
                    .consume(String.class)
                    .withOffsetReset(OffsetResetStrategy.EARLIEST)
                    .fromTopics(topicItemName).awaitRecords(1, Duration.ofSeconds(5))
                    .getRecords();
            assertThat(received).hasSize(1);
            log.infof("Consumer offset : %s",
                    companion.consumerGroups().offsets("data-replay", companion.tp(topicErrorName, 0)));

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }

    }

    //@Test TODO: fix test -> partition or use of strimzi
    public void report_getOneErrorInSummaryError() {
        try {
            var item = new ItemDto(documentClass2.getId(), "i2@id");
            String itemString = mapper.writeValueAsString(item);
            String topicErrorName = kafkaTools.getTopicErrorName(documentClass2.getName());

            var record = new ProducerRecord<>(topicErrorName, UUID.randomUUID().toString(), itemString);
            record.headers().add("exception", "error-test".getBytes(StandardCharsets.UTF_8));

            companion
                    .produce(String.class)
                    .fromRecords(record)
                    .awaitCompletion();

            Thread.sleep(5000);
            var reports = replayController.getErrorReport();
            assertThat(reports).hasSize(1);

            var report = reports.stream().filter(errorReport -> errorReport.getTopic().equals(topicErrorName)).findFirst()
                    .get();
            assertThat(report.getErrors()).hasSize(1);

        } catch (JsonProcessingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //@Test  TODO: fix test  -> partition or use of strimzi
    public void report_getZeroErrorInSummaryErrorAfterAqcuit() {
        try {
            var item = new ItemDto(documentClass3.getId(), "i3@id");
            String topicErrorName = kafkaTools.getTopicErrorName(documentClass3.getName());
            String itemString = mapper.writeValueAsString(item);
            ProducerRecord<String, String> recordError = new ProducerRecord<>(topicErrorName, UUID.randomUUID().toString(),
                    itemString);
            recordError.headers().add("exception", "error-test".getBytes(StandardCharsets.UTF_8));

            companion
                    .produce(String.class)
                    .fromRecords(recordError)
                    .awaitCompletion();

            Thread.sleep(3000);
            assertThat(replayController.getErrorReport()).hasSize(1);

            replayController.acquitError(documentClass.getId());
            var updatedReports = replayController.getErrorReport();
            var updatereport = updatedReports.stream().filter(errorReport -> errorReport.getTopic().equals(topicErrorName))
                    .findFirst().get();

            assertThat(updatereport.getErrors()).hasSize(1);

        } catch (JsonProcessingException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
