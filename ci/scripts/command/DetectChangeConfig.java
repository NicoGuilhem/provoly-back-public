//DEPS io.quarkus:quarkus-picocli
package command;

import picocli.CommandLine;

public class DetectChangeConfig {

    @CommandLine.Option(names = { "-mr",
            "--merge_request" }, description = "The merge request identifier. Can be null if pipeline executes on main branch", defaultValue = "")
    public String mrId;

    @CommandLine.Option(names = { "-mrdbs",
            "--mr-diff-base-sha" }, description = "The merge request diff base SHA. Can be null if pipeline executes on main branch", defaultValue = "")
    public String mrDiffBaseSHA;

    @CommandLine.Option(names = { "-mrs",
            "--mr-source-branch-name" }, description = "The merge request source branch name. Can be null if pipeline executes on main branch", defaultValue = "")
    public String mrSourceBranch;

    @CommandLine.Option(names = { "-mrt",
            "--mr-target-branch-name" }, description = "The merge request target branch name. Can be null if pipeline executes on main branch", defaultValue = "")
    public String mrTargetBranch;

    @CommandLine.Option(names = { "-co",
            "--commit-sha" }, description = "The commit sha. Can be null if pipeline executes on main branch", defaultValue = "")
    public String commitSha;

    @CommandLine.Option(names = { "-wr",
            "--with-ref" }, description = "Indicate if it should detect REF changes", defaultValue = "false")
    public boolean withRef;
}
