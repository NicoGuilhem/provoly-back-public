package com.provoly.link;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_COMPACT;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.provoly.common.kafka.KafkaTools;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;

import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RelationAggregateTopicCreator {
    private static String AGREGATE_TOPIC_NAME = "relation-agregate";
    Logger log;

    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    @ConfigProperty(name = "provoly.link.aggregate.partitions")
    int nbPartitions;

    KafkaTools kafkaTools;

    public RelationAggregateTopicCreator(Logger log,
            @Identifier("default-kafka-broker") Map<String, Object> kafkaConfig,
            @ConfigProperty(name = "provoly.link.aggregate.partitions") int nbPartitions,
            KafkaTools kafkaTools) {
        this.log = log;
        this.kafkaConfig = kafkaConfig;
        this.nbPartitions = nbPartitions;
        this.kafkaTools = kafkaTools;
    }

    // TODO : Duplicate from ItemsNotifier : Remove duplication
    public void buildTopic(@Observes StartupEvent ev) {

        try (var client = kafkaTools.getAdmin()) {
            if (client.listTopics().names().get()
                    .stream()
                    .noneMatch(name -> name.equals(AGREGATE_TOPIC_NAME))) {
                log.infof("Creating topic %s", AGREGATE_TOPIC_NAME);
                short replication = 1;
                NewTopic newTopic = new NewTopic(AGREGATE_TOPIC_NAME, nbPartitions, replication);
                newTopic.configs(buildRelationAggregateTopicConfig());
                client.createTopics(List.of(newTopic)).all().get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Unable to connect to kafka broker", e);
        }

    }

    private Map<String, String> buildRelationAggregateTopicConfig() {
        return Map.of(
                CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT);
    }

}
