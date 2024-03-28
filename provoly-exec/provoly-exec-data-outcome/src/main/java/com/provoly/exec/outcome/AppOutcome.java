package com.provoly.exec.outcome;

import static com.provoly.common.exec.Constants.PROVOLY_EXEC_JOB;

import java.util.UUID;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@QuarkusMain
public class AppOutcome implements QuarkusApplication {

    @Inject
    Logger log;

    @Inject
    KubernetesClient kube;

    @Inject
    PodWatcher podWatcher;

    @ConfigProperty(name = "provoly.exec.job-exec-id")
    UUID jobExecId;

    @Override
    public int run(String... args) throws Exception {
        log.info("Starting AppOutcome sidecar");
        try (var watcher = startWatcher()) {
            log.info("Watching...");
            Quarkus.waitForExit();
            log.info("Exiting AppOutcome");
            return 0;
        }
    }

    private Watch startWatcher() {
        return kube.pods()
                .withLabel(PROVOLY_EXEC_JOB, jobExecId.toString())
                .watch(podWatcher);
    }

}
