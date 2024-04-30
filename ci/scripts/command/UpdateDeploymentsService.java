//SOURCES UpdateDeploymentsConfig.java
package command;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.Dependent;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;
import org.jboss.logging.Logger;

@Dependent
public class UpdateDeploymentsService {

    public static final Logger LOG = Logger.getLogger(UpdateDeploymentsService.class);

    public static final Map<String, String> tagNames;
    static {
        tagNames = new HashMap<>();
        tagNames.put("data-ref", System.getenv().get("DATA_REF_IMAGE_TAG_NAME"));
        tagNames.put("data-virt", System.getenv().get("DATA_VIRT_IMAGE_TAG_NAME"));
        tagNames.put("data-link", System.getenv().get("DATA_LINK_IMAGE_TAG_NAME"));
        tagNames.put("data-replay", System.getenv().get("DATA_REPLAY_IMAGE_TAG_NAME"));
        tagNames.put("provoly-exec/provoly-exec-engine", System.getenv().get("PROVOLY_EXEC_ENGINE_IMAGE_TAG_NAME"));
        tagNames.put("provoly-exec/provoly-exec-file-provision", System.getenv().get("PROVOLY_EXEC_FILE_PROVISION_IMAGE_TAG_NAME"));
        tagNames.put("provoly-exec/provoly-exec-data-outcome", System.getenv().get("PROVOLY_EXEC_DATA_OUTCOME_IMAGE_TAG_NAME"));
        tagNames.put("provoly-transfo/provoly-transfo-common", System.getenv().get("PROVOLY_TRANSFO_COMMON_IMAGE_TAG_NAME"));
        tagNames.put("provoly-transfo/provoly-transfo-engine", System.getenv().get("PROVOLY_TRANSFO_ENGINE_IMAGE_TAG_NAME"));
        tagNames.put("provoly-transfo/provoly-transfo-runner", System.getenv().get("PROVOLY_TRANSFO_RUNNER_IMAGE_TAG_NAME"));
    }

    public void updateDeployments(UpdateDeploymentsConfig config) throws Exception {

        File valuesFile = new File("yap-deploy/provoly/values.yaml");
        List<String> modules = Stream.of(config.modules.split(",", -1)).collect(Collectors.toList());
        if (modules.isEmpty()) {
            LOG.warn("😴 Nothing to do!");
            return;
        } else {
            LOG.infof("👆 Modules to be updated are : \r\n - %s ", String.join("\r\n   - ", modules));
        }
        Git git = cloneProvolyDeploy(config);
        LOG.infof("📝 Amending provoly values.yaml");
        AtomicReference<String> values = new AtomicReference<>(Files.readString(valuesFile.toPath()));
        String tagValue = String.format("dev-%s", config.commitSha);
        modules.stream()
                .map(tagNames::get)
                .filter(Objects::nonNull)
                .forEach(item -> values.set(
                        values.get().replaceAll(item + ":.*", item + ": \"" + tagValue + "\"")));
        Files.writeString(valuesFile.toPath(), values.get());
        String commitMessage = modules.stream()
                .map(tagNames::get)
                .filter(Objects::nonNull)
                .map(item -> String.format("🎯 Bump %s version to %s 👏!", item, tagValue))
                .collect(Collectors.joining("\r\n"));
        git.add().addFilepattern("provoly/values.yaml").call();
        git.commit().setMessage(commitMessage).setAuthor("provoly-bot", "provoly-bot@provoly.com").call();
        push(git, config);
        LOG.infof("🦄 Updated and pushed provoly/values with %s image tag for branch %s! 🦄", tagValue, config.deployBranch);
    }

    private Git cloneProvolyDeploy(UpdateDeploymentsConfig config) throws Exception {
        LOG.infof("⚙️ Cloning provoly-deploy repository");
        return Git.cloneRepository()
                .setURI(String.format("https://gitlab.groupeonepoint.com/cds-bdx/pole-edition/yap/yap-deploy.git",
                        config.pushToken))
                .setBranch(config.deployBranch)
                .setProgressMonitor(new SimpleProgressMonitor(LOG))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", config.pushToken))
                .call();
    }

    private void push(Git git, UpdateDeploymentsConfig config) throws Exception {
        git.push()
                .setProgressMonitor(new SimpleProgressMonitor(LOG))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", config.pushToken))
                .call();
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
