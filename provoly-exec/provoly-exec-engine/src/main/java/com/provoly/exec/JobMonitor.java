package com.provoly.exec;

import static com.provoly.common.exec.Constants.PROVOLY_EXEC_JOB;
import static com.provoly.exec.JobService.JOB_CONTAINER_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.exec.ExecContext;
import com.provoly.common.exec.ExecEvent;
import com.provoly.common.exec.ExecEventKind;
import com.provoly.common.exec.ExecutionStatus;
import com.provoly.exec.model.JobExecution;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

import org.jboss.logging.Logger;

@ApplicationScoped
public class JobMonitor implements Watcher<Pod> {

    @Inject
    Logger log;

    @Inject
    KubernetesClient kube;

    @Inject
    EventEmitter eventEmitter;

    @Inject
    JobTerminatedProcessor jobTerminatedProcessor;

    @Inject
    EntityIdService repo;
    private Watch watch;

    private final Map<UUID, ExecContext> contextByExecutionId = new HashMap<>();

    //public synchronized void start(@Observes StartupEvent ev) { // Seems to call this method too early
    public synchronized void start(UUID executionId, ExecContext context) {
        if (watch == null) {
            log.info("Starting pod watcher");
            watch = kube.pods()
                    .withLabel(PROVOLY_EXEC_JOB)
                    .watch(this);
        }
        log.info("Pod watcher ready");
        contextByExecutionId.put(executionId, context);
    }

    @Transactional
    @Override
    public synchronized void eventReceived(Action action, Pod pod) {
        log.infof("JobMonitor receive event %s on pod %s", action, pod.getMetadata().getName());
        var executionId = UUID.fromString(pod.getMetadata().getLabels().get(PROVOLY_EXEC_JOB));
        log.infof("JobExecutionId %s", executionId);

        pod.getStatus().getContainerStatuses().stream()
                .filter(cs -> cs.getName().equals(JOB_CONTAINER_NAME))
                .filter(this::isTerminated)
                .filter(cs -> contextByExecutionId.containsKey(executionId))
                .findAny()
                .ifPresent(cs -> {
                    var exitCode = cs.getState().getTerminated().getExitCode();
                    var context = contextByExecutionId.get(executionId);
                    contextByExecutionId.remove(executionId);

                    log.infof("Job terminated with exit code %d", exitCode);
                    eventEmitter.jobTerminated(context, executionId);
                    var jobExecution = repo.getById(executionId, JobExecution.class);

                    if (exitCode == 0) {
                        jobExecution.setStatus(ExecutionStatus.TERMINATED);
                        jobTerminatedProcessor
                                .importDataToNewDataset(new ExecEvent(executionId, ExecEventKind.JOB_TERMINATED, context));
                    } else {
                        jobExecution.setStatus(ExecutionStatus.ERROR);
                    }
                });
    }

    @Override
    public synchronized void onClose(WatcherException cause) {
        if (watch == null) {
            log.error("Watcher stop when no watch");
        }
        watch = null;
        log.error("Pod watcher closed", cause);
    }

    /**
     * Only used by test to ensure watcher is created after mock
     */
    public synchronized void close() {
        log.info("Closing monitor");
        if (watch == null)
            return;
        watch.close();
        watch = null;
    }

    private boolean isTerminated(ContainerStatus status) {
        log.infof("Status for job container: %s", status.getState());
        return status.getState().getTerminated() != null;
    }

}
