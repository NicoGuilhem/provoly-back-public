package com.provoly.exec;

import static com.provoly.common.exec.Constants.DATA_BASE_PATH;
import static com.provoly.common.exec.Constants.ENV_NAME_JOB_EXECUTION_ID;
import static com.provoly.common.exec.Constants.PROVOLY_EXEC_JOB;
import static com.provoly.common.exec.Constants.STR_DATA_BASE_PATH;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Join;
import jakarta.transaction.Transactional;

import com.provoly.clients.DatasetService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.ExecContext;
import com.provoly.common.exec.ExecutionStatus;
import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.ref.RefChangeEvent;
import com.provoly.common.ref.RefChangeEventDatasetVersionActivated;
import com.provoly.exec.model.*;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JobService {

    // TODO : Is referencing the last version is a good idea ?
    // TODO : The registry and image should be a parameter
    static final String FILE_PROVISION_IMAGE = "dh2wltsh.gra7.container-registry.ovh.net/provoly/provoly-exec-file-provision:latest";
    static final String JOB_CONTAINER_NAME = "job";
    private static final String DATA_VOLUME_NAME = "data";
    @Inject
    Logger log;

    @Inject
    EntityIdService repo;

    @Inject
    EntityManager em;

    @Inject
    JobMapper mapper;

    @Inject
    KubernetesClient kube;

    @Inject
    EventEmitter eventEmitter;

    @Inject
    JobMonitor jobMonitor;

    @Inject
    TopicProvisionner topicProvisionner;

    @Inject
    @RestClient
    DatasetService datasetService;

    @Incoming(RefChangeEvent.TOPIC_NAME)
    @Transactional
    public void consume(RefChangeEvent event) {
        log.infof("Receiving ref change event %s", event);
        switch (event.getType()) {
            case DATASET_VERSION_ACTIVATED -> startJobInstancesWithDatasetId(
                    ((RefChangeEventDatasetVersionActivated) event).getDatasetId());
        }
    }

    private void startJobInstancesWithDatasetId(UUID datasetId) {
        var criteriaBuilder = em.getCriteriaBuilder();
        var jobInstanceCriteriaQuery = criteriaBuilder.createQuery(JobInstance.class);
        var rootQuery = jobInstanceCriteriaQuery.from(JobInstance.class);
        Join<JobInstance, DataSourceProviding> join = rootQuery.join(JobInstance_.inDataSources);

        jobInstanceCriteriaQuery.select(rootQuery).where(criteriaBuilder
                .and(
                        criteriaBuilder.equal(rootQuery.get(JobInstance_.active), true)),
                criteriaBuilder.equal(join.get(DataSourceProviding_.dataSourceId), datasetId));
        TypedQuery<JobInstance> query = em.createQuery(jobInstanceCriteriaQuery);

        List<JobInstance> result = query.getResultList();
        result.forEach(i -> start(UUID.randomUUID(), i));
    }

    @Transactional
    public JobExecutionDetailsDto start(UUID jobInstanceUuid) {
        return start(UUID.randomUUID(), jobInstanceUuid);
    }

    private JobExecution getLastJobExecution(UUID jobInstanceUuid) {
        var criteriaBuilder = em.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(JobExecution.class);
        var jobExecutionRoot = query.from(JobExecution.class);
        var jobInstance = jobExecutionRoot.join(JobExecution_.instance);
        query.where(criteriaBuilder.equal(jobInstance.get(JobInstance_.id), jobInstanceUuid));
        query.orderBy(criteriaBuilder.desc(jobExecutionRoot.get(JobExecution_.EXECUTION_DATE)));
        return em.createQuery(query).setMaxResults(1).getResultStream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "No JobExecution found for JobInstance with id %s".formatted(jobInstanceUuid)));
    }

    @Transactional
    public JobExecutionDetailsDto getLastJobExecutionForJobInstance(UUID jobInstanceUuid) {
        log.infof("Retrieving last job execution of the jobInstance with id : %s", jobInstanceUuid);
        return mapper.toDetailsDto(getLastJobExecution(jobInstanceUuid));
    }

    @Transactional
    public JobExecutionDetailsDto start(UUID jobExecutionId, UUID jobInstanceUuid) {
        var jobInstance = repo.getById(jobInstanceUuid, JobInstance.class);
        var jobExecutionDto = start(jobExecutionId, jobInstance);
        return jobExecutionDto;
    }

    private JobExecutionDetailsDto start(UUID JobExecutionId, JobInstance jobInstance) {
        var jobExecution = new JobExecution(JobExecutionId, jobInstance, ExecutionStatus.STARTED, Instant.now());
        var context = new ExecContext();
        jobMonitor.start(jobExecution.getId(), context);
        log.infof("Starting job execution %s", jobExecution);

        try {
            repo.persist(jobExecution);
            log.info("Creating in/out topics");
            createInTopics(context, jobExecution);
            prepareAndCreateOutTopics(context, jobExecution);
            eventEmitter.topicCreated(context, jobExecution.getId());

            log.info("Creating k8s job");
            var kJob = buildJob(jobExecution);
            kube.batch().v1().jobs().create(kJob.build());

            log.info("Loading data in topics");
            loadTopics(context, jobExecution);
            eventEmitter.topicLoaded(context, jobExecution.getId());
        } catch (RuntimeException exception) {
            jobExecution.setStatus(ExecutionStatus.ERROR);
            throw exception;
        }
        return mapper.toDetailsDto(jobExecution);
    }

    private void createInTopics(ExecContext context, JobExecution jobExecution) {
        for (DataSourceProviding dataSourceProviding : jobExecution.getInDataSources()) {
            switch (dataSourceProviding.getMethod()) {
                case KAFKA_TOPIC -> {
                    var topicName = topicProvisionner.createInTopic(jobExecution, dataSourceProviding);
                    context.addInTopic(dataSourceProviding.getDataSourceId(), topicName);
                }
            }
        }
    }

    private void prepareAndCreateOutTopics(ExecContext context, JobExecution jobExecution) {
        for (DatasetOutcome outDataset : jobExecution.getOutDatasets()) {
            switch (outDataset.getMethod()) {
                case KAFKA_TOPIC -> {
                    var topicName = topicProvisionner.createOutTopic(jobExecution, outDataset);
                    var datasetDto = datasetService.get(outDataset.getDatasetId());
                    context.addOutTopic(topicName, UUID.randomUUID(), datasetDto);
                }
            }
        }
    }

    private void loadTopics(ExecContext context, JobExecution jobExecution) {
        for (DataSourceProviding dataSourceProviding : jobExecution.getInDataSources()) {
            switch (dataSourceProviding.getMethod()) {
                case KAFKA_TOPIC -> topicProvisionner.loadTopic(jobExecution, dataSourceProviding);
            }
        }
    }

    private JobBuilder buildJob(JobExecution jobExecution) {

        return new JobBuilder()
                .withNewMetadata()
                .withName("job-exec-" + jobExecution.getId())
                .addToLabels(PROVOLY_EXEC_JOB, jobExecution.getId().toString())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(PROVOLY_EXEC_JOB, jobExecution.getId().toString())
                .endMetadata()
                .withSpec(buildPodSpec(jobExecution))
                .endTemplate()
                .withBackoffLimit(0)
                .endSpec();
    }

    private PodSpec buildPodSpec(JobExecution jobExecution) {

        var builder = new PodSpecBuilder()
                .withImagePullSecrets(new LocalObjectReference("regcred"))
                .addToContainers(jobContainer(jobExecution))
                .withRestartPolicy("Never")
                .withServiceAccountName("provoly-exec-file-provision");

        if (jobExecution.haveFileParameter()) {
            builder
                    .addToVolumes(dataVolume())
                    .addToInitContainers(fileProvisionContainer(jobExecution));
        }
        return builder.build();
    }

    private Container fileProvisionContainer(JobExecution jobExecution) {
        var builder = new ContainerBuilder()
                .withName("provision")
                .withImage(FILE_PROVISION_IMAGE)
                .withImagePullPolicy("Always") // TODO : Should be always only in dev mode or in provoly-dev ???
                .addToVolumeMounts(dataVolumeMount());

        addCommonEnv(builder, jobExecution);
        return builder.build();
    }

    private Container jobContainer(JobExecution jobExecution) {

        var builder = new ContainerBuilder()
                .withName(JOB_CONTAINER_NAME)
                .withImage(jobExecution.getImage())
                .withImagePullPolicy("Always") // TODO : Should be always only in dev mode or in provoly-dev ???
        ;

        addCommonEnv(builder, jobExecution);
        addFileEnv(builder, jobExecution);

        if (jobExecution.haveFileParameter()) {
            builder.addToVolumeMounts(dataVolumeMount());
        }

        return builder.build();
    }

    private void addCommonEnv(ContainerBuilder builder, JobExecution jobExecution) {
        String jobExecutionId = jobExecution.getId().toString();
        builder.addToEnv(new EnvVarBuilder().withName(ENV_NAME_JOB_EXECUTION_ID).withValue(jobExecutionId).build());
    }

    private void addFileEnv(ContainerBuilder builder, JobExecution jobExecution) {
        jobExecution.getParametersValue().forEach(pv -> {
            var envName = pv.getName();
            var filename = jobExecution.getFilename(pv);
            builder.addNewEnv().withName(envName).withValue(DATA_BASE_PATH.resolve(filename).toString()).endEnv();
        });
    }

    private VolumeMount dataVolumeMount() {
        return new VolumeMountBuilder().withName(DATA_VOLUME_NAME).withMountPath(STR_DATA_BASE_PATH).build();
    }

    private Volume dataVolume() {
        return new VolumeBuilder().withName(DATA_VOLUME_NAME).withNewEmptyDir().endEmptyDir().build();
    }

}
