package com.provoly.exec;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class JobServicePoc {

    @Inject
    Logger log;

    @Inject
    KubernetesClient kube;

    @ConfigProperty(name = "provoly.exec.job-exec-id")
    Optional<UUID> forcedJobExecId; // Only used for dev purpose

    //    public void startApp(@Observes StartupEvent event) {
    //        startJobPoc();
    //    }

    public void startJobPoc() {
        var jobExecId = forcedJobExecId.orElseGet(UUID::randomUUID);
        log.infof("Creating a job exec with uuid {}", jobExecId);

        var job = new JobBuilder()
                .withNewMetadata()
                .withName("job-exec-demo-" + jobExecId) // TODO : Allocate a name
                .addToLabels("provoly.net/provoly-exec-job-instance", jobExecId.toString())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("provoly.net/provoly-exec-job-instance", jobExecId.toString())
                .endMetadata()
                .withNewSpec()
                .withImagePullSecrets(new LocalObjectReference("regcred"))
                .addToVolumes(datasetVolumes())
                .addToInitContainers(provisionContainer())
                .addToContainers(jobContainer(), outcomeContainer(jobExecId))
                .withRestartPolicy("Never")
                .withServiceAccountName("provoly-exec-pod")
                .endSpec()
                .endTemplate()
                .withBackoffLimit(0)

                .endSpec();

        kube.batch().v1().jobs().create(job.build());

    }

    private Volume datasetVolumes() {
        return new VolumeBuilder().withName("dataset").withNewEmptyDir().endEmptyDir().build();
    }

    private Container provisionContainer() {
        return new ContainerBuilder()
                .withName("provision")
                .withImage("dh2wltsh.gra7.container-registry.ovh.net/provoly/provoly-exec-data-provision:latest")
                .addToVolumeMounts(buildVolumeMount())
                .build();
    }

    private Container jobContainer() {
        return new ContainerBuilder()
                .withName("job")
                .withImage("ubuntu:22.10")
                .withCommand("/bin/bash", "-c", "--")
                .withArgs("echo 'start job' && cat /data/in/dataset.csv && " +
                        "cp /data/in/dataset.csv /data/out/dataset.csv && " +
                        "echo '0,2,3,added_data' >> /data/out/dataset.csv && " +
                        "sleep 20 && echo 'stop job'")
                .addToVolumeMounts(buildVolumeMount())
                .build();
    }

    private Container outcomeContainer(UUID jobExecId) {
        return new ContainerBuilder()
                .withName("outcome")
                .withImage("dh2wltsh.gra7.container-registry.ovh.net/provoly/provoly-exec-data-outcome:latest")
                .addNewEnv()
                .withName("JOB_EXEC_JOB_EXEC_ID")
                .withValue(jobExecId.toString())
                .endEnv()
                .addToVolumeMounts(buildVolumeMount())
                .build();
    }

    private VolumeMount buildVolumeMount() {
        return new VolumeMountBuilder().withName("dataset").withMountPath("/data").build();
    }
}
