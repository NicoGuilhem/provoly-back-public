package com.provoly.transfo.runner;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.ExecContext;
import com.provoly.common.exec.ExecEvent;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.smallrye.common.annotation.Identifier;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExecEventConsumer {

    @Inject
    Logger log;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    private CountDownLatch topicCreated = new CountDownLatch(1);
    private CountDownLatch topicLoaded = new CountDownLatch(1);

    private Thread consumerThread;

    private ExecContext context;

    public synchronized void start(UUID jobExecutionId) {
        log.infof("Creating event consumer thread for jobExecutionId : %s", jobExecutionId);
        if (consumerThread != null) {
            String msg = "EventConsumer is already running with context" + context;
            msg += " - Unable to start for jobExecutionId " + jobExecutionId;
            throw new BusinessException(ErrorCode.TECHNICAL, msg);
        }
        consumerThread = new Thread(() -> run(jobExecutionId));
        consumerThread.start();
    }

    public ExecContext awaitTopicCreated() throws InterruptedException {
        topicCreated.await();
        return Objects.requireNonNull(context);
    }

    public void awaitTopicLoaded() throws InterruptedException {
        topicLoaded.await();
    }

    // Only used by test which reused same EventConsumer
    public synchronized void reset() throws InterruptedException {
        log.info("Resetting consumer");
        if (consumerThread != null) {
            log.info("Thread is running stop and waiting for it");
            topicLoaded.countDown();
            consumerThread.join();
            consumerThread = null;
        }
        log.info("Resetting latches");
        topicCreated = new CountDownLatch(1);
        topicLoaded = new CountDownLatch(1);
    }

    private void run(UUID jobExecutionId) {
        log.infof("Starting event consumer thread for jobExecutionId : %s", jobExecutionId);

        try (
                var keyDeserializer = new StringDeserializer();
                var valueDeserializer = new ObjectMapperDeserializer<>(ExecEvent.class);
                var kafkaConsumer = new KafkaConsumer<>(buildConfigProperties(jobExecutionId), keyDeserializer,
                        valueDeserializer)) {
            kafkaConsumer.subscribe(List.of(ExecEvent.TOPIC_NAME));
            while (topicLoaded.getCount() > 0) { // Until a topicLoaded event is received
                var events = kafkaConsumer.poll(Duration.ofSeconds(1));

                events.forEach(e -> {
                    ExecEvent event = e.value();
                    if (jobExecutionId.equals(event.jobExecutionId())) {
                        log.infof("Receiving event %d/%d: %s", e.partition(), e.offset(), event);
                        switch (event.event()) {
                            case TOPIC_CREATED -> {
                                log.info("Topic created received : opening gate");
                                context = event.context();
                                topicCreated.countDown();
                            }
                            case TOPIC_LOADED -> {
                                log.info("Topic loaded received : opening gate");
                                topicLoaded.countDown();
                            }
                        }
                    }
                });

            }
            log.info("Event consumer thread exit");

        }
    }

    /**
     * Building properties for event consumer
     * We use a dedicated group id. Every execution should receive its own start and stop
     *
     * @return All needed properties for consumer
     */
    private Properties buildConfigProperties(UUID jobExecutionId) {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaConfig.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "transfo-event-" + jobExecutionId); // TODO : If allow parallel execution for one instance every process should received start and stop)
        return consumerProperties;
    }

}
