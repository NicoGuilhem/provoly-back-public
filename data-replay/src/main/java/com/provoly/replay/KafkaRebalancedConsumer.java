package com.provoly.replay;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

@ApplicationScoped
@Identifier("summary-error.rebalancer")
public class KafkaRebalancedConsumer implements KafkaConsumerRebalanceListener {
    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        consumer.seekToBeginning(partitions);
    }
}
