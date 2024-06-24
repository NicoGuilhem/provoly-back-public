package com.provoly.exec;

import static com.provoly.common.exec.Constants.ENV_NAME_JOB_EXECUTION_ID;
import static com.provoly.common.exec.Constants.PROVOLY_EXEC_JOB;
import static com.provoly.exec.JobService.FILE_PROVISION_IMAGE;
import static com.provoly.exec.JobService.JOB_CONTAINER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.waitAtMost;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Duration;
import java.util.*;

import jakarta.inject.Inject;

import com.provoly.clients.DatasetService;
import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.exec.*;
import com.provoly.common.item.ItemDto;
import com.provoly.test.AuthService;
import com.provoly.test.DatasetFactory;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ManualStartTest extends AbstractStartTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    private JobModelDto jobModelDto;
    private JobInstanceDto jobInstanceDto;

    @InjectMock
    @RestClient
    DatasetService datasetService;

    @Inject
    AuthService authService;

    @Inject
    JobExecutionService jobExecutionService;

    @BeforeEach
    public void authenticate() {
        authService.authenticate();
    }

    /**
     * Create a simple model and a simple instance, manually start the instance and check job is started
     */
    @Test
    public void whenStart_jobIsStarted() {

        // Manually start a job
        var jobExecutionDto = startJobInstance();

        // Check that job is created
        var job = awaitJob(jobExecutionDto);
        assertThat(job.getSpec().getTemplate().getSpec().getContainers())
                .hasSize(1)
                .map(Container::getImage)
                .contains(EXPECTED_IMAGE_NAME);

        // Check that jobExecutionDto is return with instance data
        assertThat(jobExecutionDto.getInstance())
                .isNotNull()
                .extracting(JobInstanceDetailsDto::getId).isEqualTo(jobInstanceDto.getId());
    }

    /**
     * Test that start and loaded event are provided
     */
    @Test
    public void onStart_eventsAreProvided() {

        UUID jobExecutionId = UUID.randomUUID();

        log.info("Registering the mockServer endpoint for watch");
        WatchEvent watchEvent = new WatchEvent(null, "MODIFIED");
        watchEvent.setObject(new PodBuilder()
                .withNewMetadata()
                .withName("pod-mock")
                .addToLabels(PROVOLY_EXEC_JOB, jobExecutionId.toString())
                .endMetadata()
                .withNewStatus()
                .addNewContainerStatus()
                .withName(JOB_CONTAINER_NAME)
                .withNewState().withNewTerminated().withExitCode(0).endTerminated().endState()
                .endContainerStatus()
                .endStatus()
                .build());

        /*
         * Nous devons configurer le mock avant le démarrage du JobInstance afin que la websocket soit prête pour le watcher de
         * JobMonitor
         * Nous devons donc attendre 500ms avant de déclarer la 'terminaison' du pod afin que l'ordre des évènements dans le
         * topic Kafka soit respecté
         * Il ne semble pas possible d'indiquer au mock d'attendre explicitement un evènement donnée qui permette d'envoyer la
         * terminaison après
         * le démarrage du job.
         */
        mockServer.expect().get().withPath(
                "/api/v1/namespaces/test/pods?allowWatchBookmarks=true&labelSelector=provoly.net%2Fprovoly-exec-job&watch=true")
                .andUpgradeToWebSocket()
                .open()//.immediately()
                .waitFor(500)
                .andEmit(watchEvent)
                .done()
                .once();

        // Manually start a job
        var jobExecution = startJobInstance(jobExecutionId);

        var record = kafkaExecEvent
                .awaitRecords(3)
                .getRecords();

        assertThat(record)
                .map(ConsumerRecord::value)
                .map(ExecEvent::event)
                .containsExactly(ExecEventKind.TOPIC_CREATED, ExecEventKind.TOPIC_LOADED, ExecEventKind.JOB_TERMINATED);

        var jobTerminatedEvent = record.get(2).value();
        assertThat(jobTerminatedEvent.context()).isNotNull();

        // Check jobExecutionService has been updated in database
        // We need to wait for the event to be processed
        waitAtMost(Duration.ofSeconds(5))
                .until(() -> jobExecutionService.get(jobExecutionId).getStatus() == ExecutionStatus.TERMINATED);
    }

    /**
     * Test that input topic are created and data from datasource are produce in the topic
     */
    // TODO : Test ok even if exec-engine don't create topic as devservice have auto create topic enable set to true
    // https://github.com/quarkusio/quarkus/blob/4d3549fbca75e03f5d78659e366516eae8bc54f2/extensions/kafka-client/deployment/src/main/java/io/quarkus/kafka/client/deployment/RedPandaKafkaContainer.java#L60

    @Test
    public void whenInputTopic_topicCreatedAndFilled() {
        UUID datasetId = UUID.randomUUID();

        DatasetDetailsDto dto = new DatasetDetailsDto(datasetId, "dataset", UUID.randomUUID(),
                DatasetType.CLOSED, List.of(), null, List.of(), true, null, List.of());
        when(datasetService.get(datasetId)).thenReturn(dto);

        // Manually start a job

        var builder = new JobInstanceBuilder()
                .withDataSource(ProvidingMethod.KAFKA_TOPIC, DatasetFactory.BIKE_STATION_DATASOURCE_ID);

        var jobExecution = startJobInstance(builder);

        companion.consume(ItemDto.class)
                .fromTopics("transfo-in-" + jobExecution.getId() + "__" + DatasetFactory.BIKE_STATION_DATASOURCE_ID)
                .awaitRecords(DatasetFactory.BIKE_STATION_SIZE);
    }

    @Test
    public void whenOutputTopic_topicIsCreated() {

        UUID datasetId = UUID.randomUUID();

        DatasetDetailsDto dto = new DatasetDetailsDto(datasetId, "dataset", UUID.randomUUID(),
                DatasetType.CLOSED, List.of(), null, List.of(), true, null, List.of());
        when(datasetService.get(datasetId)).thenReturn(dto);

        var builder = new JobInstanceBuilder()
                .withDataOutcome(OutcomeMethod.KAFKA_TOPIC, datasetId);

        var jobExecutionDto = startJobInstance(builder);

        // Get out topic name from event
        var event = kafkaExecEvent
                .awaitRecords(2)
                .getFirstRecord().value();

        assertThat(event.event()).isEqualTo(ExecEventKind.TOPIC_CREATED);
        String outTopicName = event.context().getOutputDatasetInfo(datasetId).topicName();

        // Check topic exists
        assertThat(outTopicName).isNotNull();
        assertThat(companion.topics().list()).contains(outTopicName);

    }

    /**
     * Test that when an instance have a file input, the provoly-exec-file-provision is added as init container
     * Check also that the environment parameter is correctly set
     */
    @Test
    public void whenFileInput_fileProvisionIsAddedAsInitContainer() {

        final String PARAMETER_NAME = "name";

        var builder = new JobInstanceBuilder()
                .withParameterValue(PARAMETER_NAME, FILE_CONTENT);
        var jobExecutionDto = startJobInstance(builder, new ParameterFileDto(PARAMETER_NAME, "transfo.json"));

        var job = awaitJob(jobExecutionDto);
        var initContainers = job.getSpec().getTemplate().getSpec().getInitContainers();
        assertThat(initContainers).hasSize(1);
        var initContainer = initContainers.getFirst();
        assertThat(initContainer.getImage()).isEqualTo(FILE_PROVISION_IMAGE);
        assertThat(initContainer.getEnv())
                .anyMatch(env -> env.getName().equals(ENV_NAME_JOB_EXECUTION_ID)
                        && env.getValue().equals(jobExecutionDto.getId().toString()));
    }

    @Test
    public void whenFileInput_jobContainerHaveEnvironmentVariableWithFileName() {

        final String PARAMETER_NAME = "file_param";

        var builder = new JobInstanceBuilder()
                .withParameterValue(PARAMETER_NAME, FILE_CONTENT);
        var jobExecutionDto = startJobInstance(builder, new ParameterFileDto(PARAMETER_NAME, "transfo.json"));

        var job = awaitJob(jobExecutionDto);
        var jobContainers = job.getSpec().getTemplate().getSpec().getContainers();
        assertThat(jobContainers).hasSize(1);
        var jobContainer = jobContainers.getFirst();
        assertThat(jobContainer.getImage()).isEqualTo(EXPECTED_IMAGE_NAME);
        assertThat(jobContainer.getEnv())
                .anyMatch(env -> env.getName().equals(PARAMETER_NAME)
                        && env.getValue().equals(File.separator + "data" + File.separator + "transfo.json"));
    }

    private JobExecutionDetailsDto startJobInstance() {
        return startJobInstance(new JobInstanceBuilder());
    }

    private JobExecutionDetailsDto startJobInstance(UUID jobExecutionId) {
        return startJobInstance(jobExecutionId, new JobInstanceBuilder());
    }

    private JobExecutionDetailsDto startJobInstance(JobInstanceBuilder builder) {
        return startJobInstance(UUID.randomUUID(), builder);
    }

    private JobExecutionDetailsDto startJobInstance(UUID jobExecutionId, JobInstanceBuilder builder) {
        log.infof("Creating and starting a jobExecution id %s", jobExecutionId);
        jobModelDto = new JobModelDto(UUID.randomUUID(), EXPECTED_IMAGE_NAME);
        jobModelController.save(jobModelDto);

        jobInstanceDto = builder.build(jobModelDto.getId());
        jobInstanceController.save(jobInstanceDto);

        return jobInstanceController.start(jobExecutionId, jobInstanceDto.getId());
    }

    private JobExecutionDetailsDto startJobInstance(JobInstanceBuilder builder, ParameterDto... parameters) {
        var parametersSet = new HashSet<>(Arrays.asList(parameters));
        jobModelDto = new JobModelDto(UUID.randomUUID(), EXPECTED_IMAGE_NAME, parametersSet);
        jobModelController.save(jobModelDto);

        jobInstanceDto = builder.build(jobModelDto.getId());
        jobInstanceController.save(jobInstanceDto);

        return jobInstanceController.start(jobInstanceDto.getId());
    }

    private Job awaitJob(JobExecutionDetailsDto jobExecutionDto) {
        return await().until(() -> kube.batch().v1().jobs().withName("job-exec-" + jobExecutionDto.getId()).get(),
                Objects::nonNull);
    }

}
