///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//NATIVE_OPTIONS
//DEPS io.quarkus:quarkus-bom:3.2.4.Final@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS ch.qos.reload4j:reload4j:1.2.19
//SOURCES CustomConfiguration.java
//SOURCES Commands.java
//SOURCES command/DetectChangeService.java
//SOURCES command/DetectChangeConfig.java
//SOURCES command/DeploymentsService.java
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=INFO

import command.DetectChangeService;
import command.DetectChangeConfig;
import command.DeploymentsService;
import picocli.CommandLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Produces;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.Quarkus;

@CommandLine.Command
public class CiScripts implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "The script to execute. Valid values: ${COMPLETION-CANDIDATES}."
    )
    Commands script;

    @CommandLine.ArgGroup(exclusive=false)
    DetectChangeConfig detectChangeConfig;

    private final DetectChangeService detectChangeService;
    private final DeploymentsService deploymentsService;

    public CiScripts(DetectChangeService detectChangeService, DeploymentsService deploymentsService) {
        this.detectChangeService = detectChangeService;
        this.deploymentsService = deploymentsService;
    }

    @Override
    public void run() {
        try {
            switch (script) {
                case SELECT_TARGET_MODULES :
                {
                    detectChangeService.detectChanges(detectChangeConfig);
                    break;
                }
                case UPDATE_DEPLOYMENT  : {
                    deploymentsService.getDeployments();
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

