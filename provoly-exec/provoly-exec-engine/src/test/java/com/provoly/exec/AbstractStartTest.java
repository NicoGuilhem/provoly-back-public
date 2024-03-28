package com.provoly.exec;

import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import com.provoly.common.exec.ExecEvent;
import com.provoly.common.item.ItemDto;
import com.provoly.common.ref.RefChangeEvent;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

@WithKubernetesTestServer
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@QuarkusTestResource(ProvolyTestContainers.class)
public class AbstractStartTest {

    static final String FILE_CONTENT = "file_content";
    static final String EXPECTED_IMAGE_NAME = "imageName";

    @Inject
    Logger log;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    KubernetesClient kube;

    @Inject
    JobInstanceController jobInstanceController;

    @Inject
    JobModelController jobModelController;

    @Inject
    JobMonitor monitor;

    protected ConsumerTask<String, ExecEvent> kafkaExecEvent;

    @BeforeEach
    public void prepare(TestInfo testInfo) {
        log.infof("Preparing companion for %s", testInfo.getDisplayName());
        companion.registerSerde(RefChangeEvent.class, new ObjectMapperSerde<>(RefChangeEvent.class));
        companion.registerSerde(ItemDto.class, new ObjectMapperSerde<>(ItemDto.class));
        companion.registerSerde(ExecEvent.class, new ObjectMapperSerde<>(ExecEvent.class));
        await().during(Duration.ofMillis(500));
        createTopicIfNotExists(ExecEvent.TOPIC_NAME);
        createTopicIfNotExists(RefChangeEvent.TOPIC_NAME);

        // Start a consumer for exec event at the actual topic position
        var consumerBuilder = companion.consume(ExecEvent.class)
                .withOffsetReset(OffsetResetStrategy.LATEST);
        kafkaExecEvent = consumerBuilder.fromTopics(ExecEvent.TOPIC_NAME); // Start the consumer
        // We need to wait partition assignment before test started
        // To be sure we don't miss firsts messages
        log.info("Waiting for assignment");
        consumerBuilder.waitForAssignment().await().atMost(Duration.ofSeconds(10));

        log.infof("Companion ready. Running %s...", testInfo.getDisplayName());
    }

    private void createTopicIfNotExists(String topicName) {
        if (!topicExists(topicName)) {
            log.infof("Creating topic %s", topicName);
            companion.topics().create(topicName, 3);
            // WaitForTopic of companion throw a NPE	at io.smallrye.reactive.messaging.kafka.companion.TopicsCompanion.lambda$waitForTopic$2(TopicsCompanion.java:103)
            //  companion.topics().waitForTopic(ExecEvent.TOPIC_NAME).await().indefinitely();
            // https://github.com/smallrye/smallrye-reactive-messaging/issues/2033
            // Manual wait instead
            await().until(() -> this.topicExists(topicName));
        }
    }

    @AfterEach
    public void close() {
        monitor.close();
    }

    private boolean topicExists(String topicName) {
        return companion.topics().list().contains(topicName);
    }

}
