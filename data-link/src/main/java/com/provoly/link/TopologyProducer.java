package com.provoly.link;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.item.ItemDto;
import com.provoly.common.link.LinkDetailsDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.relation.RelationsAggregateDto;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.jboss.logging.Logger;

import kafka.KafkaTools;

@ApplicationScoped
public class TopologyProducer {

    private static final String RELATION_AGGREGATE_TOPIC_NAME = "relation-aggregate";
    @Inject
    Logger log;

    @Inject
    KafkaTools kafkaTools;

    // Inspired from https://cwiki.apache.org/confluence/display/KAFKA/KIP-150+-+Kafka-Streams+Cogroup
    // Needed workaround https://issues.apache.org/jira/browse/KAFKA-10659
    public Topology buildTopology(LinkDetailsDto link) {
        log.infof("Building a new Topology for %s", link);
        AttributeDefDetailsDto attributeSource = link.attributeSource;
        AttributeDefDetailsDto attributeDestination = link.attributeDestination;

        // Check input topics exists, unless stream is unhealthy
        String topicClassSource = buildTopicNameForClass(attributeSource);
        String topicClassDestination = buildTopicNameForClass(attributeDestination);
        if (!kafkaTools.isTopicsExists(topicClassSource, topicClassDestination)) {
            return null;
        }

        StreamsBuilder builder = new StreamsBuilder();

        Serde<ItemDto> itemSerde = new ObjectMapperSerde<>(ItemDto.class);
        Serde<RelationAggregate> relationAggregateSerde = new ObjectMapperSerde<>(RelationAggregate.class);
        Serde<RelationsAggregateDto> relationsUpdateDtoSerde = new ObjectMapperSerde<>(RelationsAggregateDto.class);

        Serde<String> stringSerde = Serdes.String();

        // Create a stream with the source linked class group by the aggregate value
        var groupedSource = builder
                .stream(topicClassSource, Consumed.with(stringSerde, itemSerde))
                .groupBy((key, value) -> extractKey(attributeSource, value),
                        Grouped.with(buildTopicNameForRepartition('S', attributeSource), stringSerde, itemSerde));

        // Create a stream with the destination linked class group by the aggregate value
        var groupedDest = builder
                .stream(topicClassDestination, Consumed.with(stringSerde, itemSerde))
                .groupBy((key, value) -> extractKey(attributeDestination, value),
                        Grouped.with(buildTopicNameForRepartition('D', attributeDestination), stringSerde, itemSerde));

        // Build a stream containing the aggregate value and all the source and destination items ids;
        groupedSource
                .cogroup(sourceAggregator)
                .cogroup(groupedDest, destAggregator)
                .aggregate(RelationAggregate::new, Materialized.with(stringSerde, relationAggregateSerde))
                .toStream()
                .map((key, value) -> KeyValue.pair(link.id + "-" + key,
                        new RelationsAggregateDto(link.id, key, value.source, value.dest)))
                .to(RELATION_AGGREGATE_TOPIC_NAME, Produced.with(stringSerde, relationsUpdateDtoSerde));

        return builder.build();

    }

    private String buildTopicNameForClass(AttributeDefDetailsDto attributeSource) {
        return "class-" + attributeSource.oclass;
    }

    private String extractKey(AttributeDefDetailsDto attribute, ItemDto value) {
        return value.getSimple(attribute.technicalName).toString();
    }

    private static Aggregator<String, ItemDto, RelationAggregate> sourceAggregator = (key, value, aggregate) -> {
        aggregate.source.add(value.getId());
        return aggregate;
    };

    private static Aggregator<String, ItemDto, RelationAggregate> destAggregator = (key, value, aggregate) -> {
        aggregate.dest.add(value.getId());
        return aggregate;
    };

    private String buildTopicNameForRepartition(char prefix, AttributeDefDetailsDto attribute) {
        String topicNameWhiteList = "[^a-zA-Z0-9\\-._]";
        return prefix + "." + attribute.name.replaceAll(topicNameWhiteList, "-");
    }

}
