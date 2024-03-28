package com.provoly.exec.outcome;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.Quarkus;

import org.jboss.logging.Logger;

@ApplicationScoped
public class PodWatcher implements Watcher<Pod> {

    @Inject
    Logger log;

    @Override
    public void eventReceived(Watcher.Action action, Pod pod) {
        log.info("Pod event received : " + action);
        pod.getStatus().getContainerStatuses().stream()
                .filter(cs -> cs.getName().equals("job"))
                .findAny()
                .filter(this::isTerminated) // Job status is not present when pod is created and before container started
                .ifPresent(jobStatus -> jobTerminated(jobStatus.getState().getTerminated()));
    }

    @Override
    public void onClose(WatcherException e) {
        log.error("Pod watcher closed", e);
    }

    private boolean isTerminated(ContainerStatus status) {
        log.infof("Status for job container: %s", status.getState());
        return status.getState().getTerminated() != null;
    }

    private void jobTerminated(ContainerStateTerminated state) {
        log.infof("Job is terminated with exit code: %s", state.getExitCode());
        Quarkus.asyncExit();
    }

}
