package com.provoly.transfo.runner;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.ExecContext;
import com.provoly.common.item.ItemDto;
import com.provoly.common.transfo.*;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.common.annotation.Identifier;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@QuarkusMain
public class Transformer implements QuarkusApplication {

    @Inject
    Logger log;
    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;
    @Inject
    ExecEventConsumer eventConsumer;
    @Inject
    ObjectMapper mapper;
    @ConfigProperty(name = "transformation.file")
    Path transfoFile;
    @ConfigProperty(name = "job.execution.id")
    UUID jobExecutionId;

    @Override
    public int run(String... args) throws Exception {
        log.infof("Start transformer for executionId : %s", jobExecutionId);
        var transfo = mapper.readValue(transfoFile.toFile(), TransfoDto.class);
        log.infof("Transfo id : %s", transfo.getId());
        // Start a thread receiving events for the transformation and opening start/stop latch when event arrive
        eventConsumer.start(jobExecutionId);

        log.info("Waiting for start event");
        var context = eventConsumer.awaitTopicCreated();
        log.infof("Context received %s", context);

        log.info("Constructing topology");
        var graph = new TransfoGraph(transfo);
        var builder = new StreamsBuilder();

        Map<NodeDto, KStream<String, ItemDto>> streamsMap = new HashMap<>();

        for (NodeDto node : graph.ordered()) {
            switch (node.getSpec()) {
                case Filter filterSpec -> {
                    log.infof("filter stream %s", filterSpec.getAttributeName());
                    var previousNode = graph.getPrevious(node);
                    var inputStream = streamsMap.get(previousNode);
                    var outStream = buildOutstream(inputStream, filterSpec);
                    streamsMap.put(node, outStream);
                }
                case NoOp ignored -> {
                    var previousNode = graph.getPrevious(node);
                    var inputStream = streamsMap.get(previousNode);
                    var outputStream = inputStream;
                    streamsMap.put(node, outputStream);
                }
                case InputDatasource inDataSource -> {
                    String inTopicName = context.getInTopicName(inDataSource.getDatasetId());
                    String processorName = "pump-" + inDataSource.getDatasetId();
                    log.infof("inputdatasource stream %s", processorName);
                    KStream<String, ItemDto> outStream = builder.stream(inTopicName, Consumed.as(processorName));
                    streamsMap.put(node, outStream);
                }
                case OutputDataset outDataset -> {
                    var outputDatasetInfo = context.getOutputDatasetInfo(outDataset.getDataset());
                    log.infof("outputdataset stream %s", outputDatasetInfo.datasetDetailDto().getName());
                    var previousNode = graph.getPrevious(node);
                    var inputStream = streamsMap.get(previousNode);
                    var outStream = buildOutstream(inputStream, outDataset, context);

                    String processorName = "write-" + outDataset.getDataset();
                    var outTopicName = outputDatasetInfo.topicName();

                    outStream.to(outTopicName, Produced.as(processorName));
                }
                default -> throw new BusinessException(ErrorCode.TECHNICAL, "Unknown node type : " + node);
            }
        }

        var topology = builder.build();

        log.infof("Starting streams for topology %s", topology.describe());
        final KafkaStreams streams = new KafkaStreams(topology, buildStreamProperties(transfo));
        streams.cleanUp(); // TODO : Remove

        streams.start();
        log.info("Waiting for stop event");
        eventConsumer.awaitTopicLoaded();

        log.info("Waiting for lag down to 0");
        while (!detectEnd(streams)) {
            Thread.sleep(300);
        }

        log.info("Closing stream");
        streams.close();
        log.info("All done - Transformer quit!!!");
        return 0;
    }

    public Properties buildStreamProperties(TransfoDto transfo) {
        Properties streamsProperties = new Properties();
        streamsProperties.putAll(kafkaConfig);
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "transfo-" + transfo.getId());
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, ItemDtoSerde.class);
        return streamsProperties;
    }

    private KStream<String, ItemDto> buildOutstream(KStream<String, ItemDto> inputStream, Filter filter) {
        return inputStream.filter((key, value) -> {
            String attributeValue = value.getSimple(filter.getAttributeName()).toString();
            String filterValueAsString = filter.getValue().toString();

            return switch (filter.getOperator()) {
                case EQUALS -> value.getSimple(filter.getAttributeName()).equals(filter.getValue());
                case NOT_EQUALS -> !(value.getSimple(filter.getAttributeName()).equals(filter.getValue()));
                case CONTAINS -> attributeValue.contains(filterValueAsString);
                case GREATER_THAN -> Integer.parseInt(attributeValue) > Integer.parseInt(filterValueAsString);
                case LOWER_THAN -> Integer.parseInt(attributeValue) < Integer.parseInt(filterValueAsString);
                case START_WITH -> attributeValue.startsWith(filterValueAsString);
                case END_WITH -> attributeValue.endsWith(filterValueAsString);
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Operator OUTSIDE, DISTANCE and INTERSECTS are not implemented for now.");
            };
        });
    }

    private KStream<String, ItemDto> buildOutstream(KStream<String, ItemDto> inputStream, OutputDataset dataset,
            ExecContext context) {
        return inputStream.map((key, value) -> {
            var outputInfo = context.getOutputDatasetInfo(dataset.getDataset());
            var item = new ItemDto(
                    outputInfo.datasetDetailDto().getoClass(),
                    outputInfo.datasetId(),
                    value.getItemId(),
                    value.getAttributes());
            return KeyValue.pair(item.getId(), item);
        });
    }

    private boolean detectEnd(KafkaStreams streams) {
        log.info("Check lag");
        return getTotalLag(streams) == 0;
    }

    private Long getTotalLag(KafkaStreams streams) {
        Long totalLag = streams.metrics().entrySet().stream()
                .filter(entry -> entry.getKey().name().equals("records-lag"))
                .filter(entry -> entry.getKey().tags().containsKey("partition"))
                .map(entry -> ((Number) entry.getValue().metricValue()).longValue())
                .reduce(-1L, (old, current) -> old == -1l ? current : current + old);
        if (totalLag >= 0L) {
            log.infof("Detect lag of %d", totalLag);
        } else {
            log.debug("No metrics");
        }
        return totalLag;
    }
}
