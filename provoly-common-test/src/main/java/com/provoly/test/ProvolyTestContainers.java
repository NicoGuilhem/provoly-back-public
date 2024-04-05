package com.provoly.test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

/**
 * This class is only used by DATA_VIRT TEST
 * Start every container needed for test
 * Every container is started in a dedicated network
 */

public class ProvolyTestContainers implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
    String imagePolicy = System.getProperty("service.image.policy", "alwaysPull");
    String fileSystemBind = System.getProperty("service.filesystem.bind", "/home/jboss/config/application.yaml");
    String fullImageName = System.getProperty("service.fullImage.name",
            "dh2wltsh.gra7.container-registry.ovh.net/provoly/data-ref:latest");

    public static final String POSTGRES_SERVICE_LABEL = "provoly-postgres";
    public static final String POSTGRES_VALUE = "postgis";
    public static final String POSTGRES_DATABASE = "provoly";
    public static final String POSTGRES_USERNAME = "postgres_admin";
    public static final String POSTGRES_PASSWORD = "password";
    public static final String POSTGRES_DATAREF_USERNAME = "dataref";
    public static final String POSTGRES_DATAREF_PASSWORD = "dataref";
    public static final int POSTGRES_EXTERNAL_PORT = 5430;

    public static final String ELASTIC_SERVICE_LABEL = "provoly-elastic";
    public static final String ELASTIC_VALUE = "elastic";
    public static final int ELASTIC_PORT = 9200;

    public static final String KAFKA_SERVICE_LABEL = "quarkus-dev-service-kafka";
    public static final String KAFKA_VALUE = "kafka";
    public static final int KAFKA_PORT = 9092;
    private static final String KAFKA_DOCKER_IMAGE = "dh2wltsh.gra7.container-registry.ovh.net/docker-mirror/vectorized/redpanda:v22.3.4";

    public static final String DEV_SERVICE_LABEL = "provoly-dataref";
    public static final String DEV_VALUE = "dataref";
    public static final int DATA_REF_PORT = 8180;

    private static final Logger log = Logger.getLogger(ProvolyTestContainers.class);

    private DevServicesContext context;

    private Network sharedNetwork = Network.newNetwork();

    private GenericContainer<?> postgresContainer = buildPostgresContainer();
    private GenericContainer<?> elasticContainer = buildElasticContainer();
    private RedpandaTestContainer kafkaContainer = buildKafkaContainer();
    private KeycloakContainer keycloakContainer = buildKeycloakContainer();
    private GenericContainer<?> dataRefContainer = buildDataRefContainer();

    @Override
    public Map<String, String> start() {

        log.info("Starting all containers...");

        sharedNetwork = Network.newNetwork();

        postgresContainer = buildPostgresContainer();
        elasticContainer = buildElasticContainer();
        kafkaContainer = buildKafkaContainer();
        keycloakContainer = buildKeycloakContainer();
        dataRefContainer = buildDataRefContainer();

        log.info("Starting all support containers");
        Startables
                .deepStart(postgresContainer, elasticContainer, kafkaContainer, keycloakContainer)
                .join();
        log.info("Initializing keycloak");
        keycloakContainer.postInit(); // Can't use depends on because of the postInit method
        log.info("Starting data ref container");
        dataRefContainer.start();

        var config = new HashMap<String, String>();
        config.put("provoly.virt.elasticsearch.host", elasticContainer.getHost());
        config.put("provoly.virt.elasticsearch.port", String.valueOf(elasticContainer.getMappedPort(ELASTIC_PORT)));
        config.put("provoly.virt.elasticsearch.protocol", "http");
        config.put("provoly.virt.elasticsearch.username", ELASTIC_VALUE);
        config.put("provoly.virt.elasticsearch.password", ELASTIC_VALUE);
        config.put("quarkus.rest-client.data-ref.url",
                "http://" + dataRefContainer.getHost() + ":" + dataRefContainer.getFirstMappedPort());
        config.put("kafka.bootstrap.servers", kafkaContainer.getHost() + ":" + kafkaContainer.getFirstMappedPort());

        // Parameters for keycloak
        String keycloakFromDockerHost = "http://127.0.0.1:%s".formatted(keycloakContainer.getFirstMappedPort());
        String keycloakRealmFromDockerHost = keycloakFromDockerHost + "/realms/provoly";
        config.put("quarkus.oidc.auth-server-url", keycloakRealmFromDockerHost);
        config.put("quarkus.oidc-client.auth-server-url", keycloakRealmFromDockerHost);
        config.put("quarkus.rest-client.\"sso\".url", keycloakFromDockerHost);

        context.devServicesProperties().putAll(config);

        return config;
    }

    @Override
    public void stop() {
        log.info("Stopping all containers...");
        postgresContainer.stop();
        elasticContainer.stop();
        kafkaContainer.stop();
        keycloakContainer.stop();
        dataRefContainer.stop();
        sharedNetwork.close();
        log.info("Containers stopped.");
    }

    private GenericContainer<?> buildPostgresContainer() {
        var postgres = new PostgisContainerProvider().newInstance()
                .withUsername(POSTGRES_USERNAME)
                .withPassword(POSTGRES_PASSWORD)
                .withDatabaseName(POSTGRES_DATABASE)
                .withInitScript("postgis-init.sql")
                .withLabel(POSTGRES_SERVICE_LABEL, POSTGRES_VALUE)
                .withNetwork(sharedNetwork)
                .withNetworkAliases("db01");

        postgres.setDockerImageName("dh2wltsh.gra7.container-registry.ovh.net/docker-mirror/postgis/postgis:16-3.4");
        postgres.setPortBindings(List.of("%s:%s".formatted(POSTGRES_EXTERNAL_PORT, "5432")));
        return postgres;
    }

    private GenericContainer<?> buildElasticContainer() {
        var elasticContainer = new GenericContainer<>(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.4.1"))
                .withLabel(ELASTIC_SERVICE_LABEL, ELASTIC_VALUE)
                .withNetwork(sharedNetwork)
                .withExposedPorts(ELASTIC_PORT)
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withClasspathResourceMapping("elasticsearch.yml",
                        "/usr/share/elasticsearch/config/elasticsearch.yml", BindMode.READ_ONLY);

        elasticContainer.setPortBindings(List.of("%s:%s".formatted(ELASTIC_PORT, ELASTIC_PORT)));
        elasticContainer
                .setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.of(3L, ChronoUnit.MINUTES)));
        return elasticContainer;
    }

    private RedpandaTestContainer buildKafkaContainer() {
        var kafkaTestContainer = new RedpandaTestContainer(DockerImageName.parse(KAFKA_DOCKER_IMAGE), KAFKA_PORT,
                KAFKA_SERVICE_LABEL)
                .withLabel(KAFKA_SERVICE_LABEL, KAFKA_VALUE)
                .withNetwork(sharedNetwork)
                .withNetworkAliases("kafka")
                .withExposedPorts(KAFKA_PORT);
        return kafkaTestContainer;
    }

    private KeycloakContainer buildKeycloakContainer() {
        var keycloakContainer = new KeycloakContainer("dev-realm.json")
                .withNetwork(sharedNetwork)
                .withNetworkAliases("sso")
                .withLogConsumer(
                        // Using sout to avoid bad log formatting
                        outputFrame -> System.out.print("KEYCLOAK: " + outputFrame.getUtf8String()));
        return keycloakContainer;
    }

    private GenericContainer<?> buildDataRefContainer() {
        var imagePullPolicy = imagePolicy.equals("alwaysPull") ? PullPolicy.alwaysPull() : PullPolicy.defaultPolicy();
        var waitLogFor = ".*Listening on: http:\\/\\/0\\.0\\.0\\.0:" + DATA_REF_PORT + ".*";
        var dataRefContainer = new GenericContainer<>(DockerImageName.parse(fullImageName))
                .withLabel(DEV_SERVICE_LABEL, DEV_VALUE)
                .withNetwork(sharedNetwork)
                .withImagePullPolicy(imagePullPolicy)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(waitLogFor))
                .withExposedPorts(DATA_REF_PORT)
                .withLogConsumer(
                        // Using sout to avoid bad log formatting
                        (Consumer<OutputFrame>) outputFrame -> System.out.print("DATA_REF: " + outputFrame.getUtf8String()))
                .withClasspathResourceMapping("application-container-data-ref.yaml", fileSystemBind, BindMode.READ_ONLY)
                .withEnv("quarkus.datasource.db-kind", "postgresql")
                .withEnv("quarkus.datasource.username", POSTGRES_DATAREF_USERNAME)
                .withEnv("quarkus.datasource.password", POSTGRES_DATAREF_PASSWORD)
                .withEnv("quarkus.datasource.jdbc.url",
                        "jdbc:postgresql://db01:5432/" + POSTGRES_DATABASE + "?currentSchema=dataref")
                .withEnv("quarkus.oidc.auth-server-url", "http://sso:8080/realms/provoly")
                .withEnv("quarkus.rest-client.\"sso\".url", "http://sso:8080/realms/provoly")
                .withEnv("kafka.bootstrap.servers", "kafka:29092");
        return dataRefContainer;
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }
}
