package com.provoly.replay;

import java.util.Properties;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.provoly.common.kafka.KafkaTools;
import com.provoly.replay.entity.ErrorLine;
import com.provoly.replay.entity.ErrorReport;
import com.provoly.replay.serializer.ErrorReportDeserializer;
import com.provoly.replay.serializer.ErrorReportSerde;
import com.provoly.replay.serializer.ErrorReportSerializer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SummaryProcessor {

    @Inject
    Logger log;

    @Inject
    KafkaTools kafkaTools;

    @ConfigProperty(name = "provoly.replay.commit_interval_ms", defaultValue = "30000")
    String commitIntervalMs;

    @ConfigProperty(name = "provoly.replay.probing_rebalance_interval_ms", defaultValue = "600000")
    String probingRebalanceIntervaleMs;

    @ConfigProperty(name = "provoly.replay.skipping_fake_dlq_creation", defaultValue = "false")
    String skippingFakeDlqCreation;

    private KafkaStreams errorStream;

    private StreamsBuilder streamsBuilder = new StreamsBuilder();

    public void readErrorMessage(@Observes StartupEvent ev) {
        if (!Boolean.parseBoolean(skippingFakeDlqCreation)) {
            kafkaTools.createTopicIfNeeded("dlq-ds-fake_dlq");
        }
        Serde<ErrorLine> lineSerde = new ObjectMapperSerde<>(ErrorLine.class);
        Serde<ErrorReport> reportSerde = Serdes.serdeFrom(new ErrorReportSerializer(), new ErrorReportDeserializer());

        KStream<String, String> exceptions = streamsBuilder.stream(Pattern.compile("dlq-ds-.*"),
                Consumed.with(Serdes.String(), Serdes.String()));
        exceptions
                .transformValues(ToErrorLine::new)
                .groupBy((key, value) -> value.getTopic(), Grouped.with(Serdes.String(), lineSerde))
                .aggregate(ErrorReport::new, (key, value, aggregate) -> aggregate.accumulate(value),
                        Materialized.as(ErrorService.STORE_NAME))
                .toStream().to(ErrorService.SUMMARY_TOPIC, Produced.with(Serdes.String(), reportSerde));

        Properties prop = kafkaTools.buildStreamProperties(ErrorService.STREAM_ID);
        prop.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, ErrorReportSerde.class);
        prop.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitIntervalMs);
        prop.put(StreamsConfig.PROBING_REBALANCE_INTERVAL_MS_CONFIG, probingRebalanceIntervaleMs);

        errorStream = new KafkaStreams(streamsBuilder.build(), prop);
        errorStream.start();
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping Kafka Streams pipeline");
        errorStream.close();
    }

}
