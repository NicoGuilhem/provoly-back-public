package command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;
import jakarta.enterprise.context.Dependent;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;
import org.jboss.logging.Logger;

@Dependent
public class DeploymentsService {

    public static final Logger LOG = Logger.getLogger(DeploymentsService.class);

    public static final Map<String, String> tagNames;
    static {
        tagNames = new HashMap<>();
        tagNames.put("data-ref", "dataRefImagetag");
        tagNames.put("data-virt", "dataVirtImagetag");
        tagNames.put("data-link", "dataLinkImagetag");
        tagNames.put("data-sync", "dataSyncImagetag");
        tagNames.put("data-replay", "dataReplayImagetag");
        tagNames.put("provoly-exec/provoly-exec-engine", "provolyExecEngineImagetag");
        tagNames.put("provoly-exec/provoly-exec-file-provision", "provolyExecFileProvisionImagetag");
        tagNames.put("provoly-exec/provoly-exec-data-outcome", "provolyExecDataOutcomeImagetag");
        tagNames.put("provoly-transfo/provoly-transfo-engine", "provolyTransfoEngineImagetag");
        tagNames.put("provoly-transfo/provoly-transfo-runner", "provolyTransfoRunnerImagetag");
    }

    public void getDeployments() throws Exception {
        var changeService = new DetectChangeService();
        var detect_config = new DetectChangeConfig();
        detect_config.mrId = "";
        detect_config.withRef= true;
        var modules = changeService.detectChanges(detect_config);

        if (modules.isEmpty()) {
            LOG.warn("😴 Nothing to do!");
            return;
        } else {
            LOG.infof("👆 Modules to be updated are : \r\n - %s ", String.join("\r\n   - ", modules));
        }

        var toDeploy = tagNames.entrySet().stream()
                .filter(entry -> modules.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        Files.writeString(Path.of(
                "deployment.env"), "DEPLOY=" + String.join(",", toDeploy));
    }

    class SimpleProgressMonitor implements ProgressMonitor {

        private Logger log;

        SimpleProgressMonitor(Logger log) {
            this.log = log;
        }

        public void start(int totalTasks) {
            log.infof("Starting work on %s tasks.", totalTasks);
        }

        public void beginTask(String title, int totalWork) {
            log.infof("Start %s : %s ", title, totalWork);
        }

        public void update(int completed) {
            // ignored
        }

        public void endTask() {
            log.info("Done");
        }

        public boolean isCancelled() {
            return false;
        }

        public void showDuration(boolean enabled) {
            // ignored here
        }
    }
}
