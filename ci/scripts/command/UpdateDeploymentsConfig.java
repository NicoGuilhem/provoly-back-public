//DEPS io.quarkus:quarkus-picocli
package command;

import picocli.CommandLine;

import java.util.List;

public class UpdateDeploymentsConfig {

    @CommandLine.Option(names = { "-m", "--modules" }, description = "The comma separated list of modules to be impacted")
    public String modules;

    @CommandLine.Option(names = { "-c", "--commit-sha1" }, description = "The commit sha. Can be null if pipeline executes on main branch", defaultValue = "")
    public String commitSha;

    @CommandLine.Option(names = { "-pt", "--push-token" }, description = "The git repository push token")
    public String pushToken;

    @CommandLine.Option(names = { "-db", "--deploy-branch" }, description = "The provoly-deploy target branch", defaultValue = "main")
    public String deployBranch;
}
