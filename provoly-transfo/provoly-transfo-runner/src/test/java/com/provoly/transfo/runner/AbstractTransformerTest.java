package com.provoly.transfo.runner;

import static com.provoly.common.exec.ExecEvent.TOPIC_NAME;
import static com.provoly.test.DatasetFactory.BIKE_STATION_DATASOURCE_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.exec.ExecContext;
import com.provoly.common.exec.ExecEvent;
import com.provoly.common.exec.ExecEventKind;
import com.provoly.common.item.AttributeSimpleValueDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.transfo.*;
import com.provoly.test.ItemsServiceMock;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.ObjectMapper;

@QuarkusTestResource(KafkaCompanionResource.class)
public abstract class AbstractTransformerTest {
    @Inject
    Logger log;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    Transformer transformer;

    @Inject
    ExecEventConsumer eventConsumer;

    @Inject
    @RestClient
    ItemsServiceMock itemsService;

    @ConfigProperty(name = "transformation.file")
    Path transfoFile;

    @ConfigProperty(name = "job.execution.id")
    UUID jobExecutionId;

    @Inject
    ObjectMapper mapper;

    protected ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<Integer> threadResult;

    protected UUID transfoUuid;

    /** Input and output node when transformation is build by AbstractTransformerTest */
    private NodeDto inputNode;
    private NodeDto outputNode;

    @BeforeEach
    void prepare() throws InterruptedException {
        transfoUuid = UUID.randomUUID();
        companion.registerSerde(ExecEvent.class, new ObjectMapperSerde<>(ExecEvent.class));
        companion.registerSerde(ItemDto.class, new ObjectMapperSerde<>(ItemDto.class));

        // TODO : When multiple test in one test class, same execEventConsumer are reused.
        //  Check if a new Transformer can be injected
        eventConsumer.reset();

        log.info("Checking topic exec event exists");
        if (!companion.topics().list().contains(ExecEvent.TOPIC_NAME)) {
            log.infof("Creating topics %s", ExecEvent.TOPIC_NAME);
            companion.topics().create(ExecEvent.TOPIC_NAME, 1);
        }

    }

    @AfterEach
    void WaitingForRunnerToStopAndClean() throws InterruptedException, IOException, ExecutionException, TimeoutException {
        log.info("Waiting for transformer end");
        threadResult.get(20, TimeUnit.SECONDS);
        Files.delete(transfoFile);
        log.info("Test finished");
    }

    protected void runTransfo(TransfoDto transfo) {
        startTransfoRunner(transfo);
        sendStartEvent(transfo);
        sendAllRecords(transfo);
        sendStopEvent(transfo);
    }

    protected TransfoDto buildTransfo(NodeSpec... specs) {

        var nodes = new ArrayList<NodeDto>();

        var inputDataSource = new InputDatasource(BIKE_STATION_DATASOURCE_ID);
        inputNode = new NodeDto(inputDataSource);
        nodes.add(inputNode);

        Arrays.stream(specs).forEach(spec -> nodes.add(new NodeDto(spec)));

        var outDataSource = new OutputDataset(UUID.randomUUID());
        outputNode = new NodeDto(outDataSource);
        nodes.add(outputNode);

        return TransfoDto.withLinkGeneration(transfoUuid, nodes, "Default Title");

    }

    protected void startTransfoRunner(TransfoDto transfo) {
        try {
            log.info("Creating input topics");
            transfo.forEach(InputDatasource.class, inputDataSource -> {
                String sourceTopicName = getInTopicName(inputDataSource);
                log.infof("Creating topics %s", sourceTopicName);
                companion.topics().createAndWait(sourceTopicName, 3);
            });

            log.infof("Serialize transfo to file and start transformer for %s", transfo.getId());
            mapper.writerFor(TransfoDto.class).writeValue(transfoFile.toFile(), transfo);

            threadResult = executorService.submit(() -> transformer.run());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void sendStartEvent(TransfoDto transfo) {
        log.info("Producing start event");

        log.info("Preparing and sending start context");
        var execContext = new ExecContext();
        transfo.forEach(InputDatasource.class, inputDataSource -> {
            execContext.addInTopic(inputDataSource.getDatasetId(), getInTopicName(inputDataSource));
        });

        transfo.forEach(OutputDataset.class, outputDataset -> {
            execContext.addOutTopic(getOutTopicName(outputDataset.getDataset()),
                    UUID.randomUUID(),
                    new DatasetDetailsDto(outputDataset.getDataset(), "dataset", UUID.randomUUID(),
                            DatasetType.CLOSED, List.of(), null, List.of(), false, null, List.of()));
        });

        var startEventRecord = new ProducerRecord<String, ExecEvent>(TOPIC_NAME,
                new ExecEvent(jobExecutionId, ExecEventKind.TOPIC_CREATED, execContext));
        companion.produce(ExecEvent.class).fromRecords(startEventRecord).awaitCompletion();
    }

    protected void sendAllRecords(TransfoDto transfo) {
        transfo.forEach(InputDatasource.class, inputDatasource -> sendRecords(buildRecords(inputDatasource)));
    }

    protected void sendStopEvent(TransfoDto transfo) {
        log.info("Producing end event");
        var stopEventRecord = new ProducerRecord<String, ExecEvent>(TOPIC_NAME,
                new ExecEvent(jobExecutionId, ExecEventKind.TOPIC_LOADED, null));
        companion.produce(ExecEvent.class).fromRecords(stopEventRecord).awaitCompletion();
    }

    protected List<ProducerRecord<String, ItemDto>> buildRecords() {
        return buildRecords(inputNode.specAs(InputDatasource.class));
    }

    protected List<ProducerRecord<String, ItemDto>> buildRecords(InputDatasource inputDataSource) {
        return itemsService.getItems(inputDataSource.getDatasetId()).items()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(item -> new ProducerRecord<>(getInTopicName(inputDataSource), UUID.randomUUID().toString(), item))
                .collect(Collectors.toList());
    }

    protected void sendRecords(List<ProducerRecord<String, ItemDto>> records) {
        companion.produce(ItemDto.class).fromRecords(records).awaitCompletion();
    }

    protected List<ConsumerRecord<String, ItemDto>> getResults() {
        return getResults(List.of(outputNode.specAs(OutputDataset.class).getDataset()));
    }

    protected List<ConsumerRecord<String, ItemDto>> getResults(List<UUID> dataSourceDefinition) {
        Set<String> destinationTopicNames = getOutTopicName(dataSourceDefinition);
        log.infof("Test are consuming results for topic: %s", String.join(", ", destinationTopicNames));
        return companion.consume(ItemDto.class)
                .fromTopics(destinationTopicNames, untilRunnerQuit())
                .awaitCompletion()
                .getRecords();
    }

    protected int getRunnerReturnCode(Duration duration) {
        try {
            return threadResult.get(duration.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getFreeSpaceValue(ItemDto itemDto) {
        return itemDto
                .getAttributes()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals("freeSpace"))
                .map(entry -> ((AttributeSimpleValueDto) entry.getValue()).value.toString())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException());
    }

    private <K, V> Function<Multi<ConsumerRecord<K, V>>, Multi<ConsumerRecord<K, V>>> untilRunnerQuit() {
        return multi -> new MultiUntilThreadQuitOp<>(multi, threadResult);
    }

    private String getInTopicName(InputDatasource inputDataSource) {
        return "topic-test-in-" + transfoUuid + "-" + inputDataSource.getDatasetId();
    }

    protected String getOutTopicName(UUID dataSourceDefinition) {
        return "topic-test-out-" + transfoUuid + "-" + dataSourceDefinition;
    }

    private Set<String> getOutTopicName(List<UUID> dataSourceDefinitions) {
        return dataSourceDefinitions.stream()
                .map(this::getOutTopicName)
                .collect(Collectors.toSet());
    }

}
