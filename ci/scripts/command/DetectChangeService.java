//DEPS io.quarkiverse.jgit:quarkus-jgit:3.0.0
package command;

import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.revwalk.*;
import org.jboss.logging.Logger;

@Dependent
public class DetectChangeService {

    public static final Logger LOG = Logger.getLogger(DetectChangeService.class);

    public static final List<String> build_all = List.of(
            ".gitlab-ci.yml",
            "provoly-common",
            "provoly-common-test",
            "provoly-parent",
            "provoly-parent-build-quarkus");

    public static final List<String> modules = List.of(
            "data-virt",
            "data-link",
            "data-replay",
            "provoly-exec/provoly-exec-engine",
            "provoly-exec/provoly-exec-file-provision",
            "provoly-exec/provoly-exec-data-outcome",
            "provoly-transfo/provoly-transfo-common",
            "provoly-transfo/provoly-transfo-engine",
            "provoly-transfo/provoly-transfo-runner");

    public static final String refModule = "data-ref";

    public List<String> detectChanges(DetectChangeConfig config) throws Exception {
        Git git = Git.open(new File("."));
        Repository repo = git.getRepository();
        ObjectReader reader = repo.newObjectReader();
        List<String> entries = null;

        List<String> modulesList = new ArrayList<String>();
        modulesList.addAll(modules);
        if (config.withRef) {
            modulesList.add(refModule);
        }
        if (!config.mrId.isEmpty()) {
            LOG.infof("🛠️Working on merge request");
            LOG.infof("🚀Detecting changes for MR between %s and %s", config.mrTargetBranch, config.mrSourceBranch);
            entries = git
                    .diff()
                    .setShowNameAndStatusOnly(true)
                    .setNewTree(prepareTreeParser(repo, config.mrDiffBaseSHA))
                    .setOldTree(prepareTreeParser(repo, config.commitSha))
                    .call()
                    .stream()
                    .map(DiffEntry::getNewPath)
                    .collect(Collectors.toList());
        } else {
            LOG.infof("🛠️Working on main branch");
            LOG.infof("🚀 Detecting changes for main branch between head and previous commit");

            entries = git
                    .diff()
                    .setShowNameAndStatusOnly(true)
                    .setNewTree(prepareTreeParser(repo, "HEAD"))
                    .setOldTree(prepareTreeParser(repo, "HEAD~1"))
                    .call()
                    .stream()
                    .map(DiffEntry::getNewPath)
                    .collect(Collectors.toList());
        }
        String entriesAsString = String.join(",", entries);

        if (build_all.stream().anyMatch(entriesAsString::contains)) {
            writeTargetModules(modulesList);
            return modulesList;
        }

        List<String> targetModules = modulesList.stream()
                .filter(entriesAsString::contains)
                .collect(Collectors.toList());

        if (!targetModules.isEmpty()) {
            writeTargetModules(targetModules);
            return targetModules;
        }
        LOG.info("Nothing else to build, we will pretend as if we do something 😱");
        writeTargetModules(List.of("provoly-parent"));
        return List.of();
    }

    private static void writeTargetModules(List<String> modules) throws IOException {
        LOG.infof("🎯 Target modules are : \n\t- %s", String.join("\r\n\t- ", modules));

        Files.writeString(Path.of(
                "build.env"), "MODULES=" + String.join(",", modules));
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws Exception {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }
}
