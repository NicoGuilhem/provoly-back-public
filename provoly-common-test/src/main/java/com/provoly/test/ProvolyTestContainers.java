package com.provoly.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.ExecCreateCmd;

/**
 * Data-virt Test use dataRef container which need a Postgres and a Kafka to run
 */

public class ProvolyTestContainers implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
    String imagePolicy = System.getProperty("service.image.policy", "alwaysPull");
    String fileSystemBind = System.getProperty("service.filesystem.bind", "/home/jboss/config/application.yaml");
    String fullImageName = System.getProperty("service.fullImage.name",
            "dh2wltsh.gra7.container-registry.ovh.net/provoly/data-ref:latest");

    public static final String POSTGRES_SERVICE_LABEL = "provoly-postgres";
    public static final String POSTGRES_VALUE = "postgres";
    public static final String USERNAME = "test";
    public static final String PASSWORD = "test";
    public static final String DATABASE_NAME = "postgres";
    public static final int POSTGRES_PORT = 5432;

    public static final String POSTGIS_SERVICE_LABEL = "provoly-postgis";
    public static final String POSTGIS_VALUE = "postgis";
    public static final String USERNAME_POSTGIS = "test_postgis";
    public static final String PASSWORD_POSTGIS = "test_postgis";
    public static final String DATABASE_NAME_POSTGIS = "postgis";
    public static final int POSTGIS_PORT = 5430;

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

    private ExecCreateCmd postgresCleanUp;
    private ExecCreateCmd postgisCleanUp;
    private ExecCreateCmd elasticCleanUp;

    private ExecCreateCmd kafkaCleanUp;
    private ExecCreateCmd keycloakCleanUp;
    private ExecCreateCmd datarefCleanUp;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private static final String DROP_TABLE = """
            psql -U postgres -d postgres -c '
            DO $$
            DECLARE
                r record;
            BEGIN
                FOR r IN SELECT quote_ident(tablename) AS tablename, quote_ident(schemaname) AS schemaname FROM pg_tables WHERE schemaname = 'public'
                LOOP
                    RAISE INFO 'truncate table %.%', r.schemaname, r.tablename;
                    EXECUTE format('TRUNCATE TABLE %I.%I CASCADE', r.schemaname, r.tablename);
                END LOOP;
            END$$;
            '""";

    private static final String DROP_TABLE_POSTGIS = """
            psql -U postgis -d postgis -c '
            DO $$
            DECLARE
                r record;
            BEGIN
                FOR r IN SELECT quote_ident(tablename) AS tablename, quote_ident(schemaname) AS schemaname FROM pg_tables WHERE schemaname = 'public'
                LOOP
                    RAISE INFO 'truncate table %.%', r.schemaname, r.tablename;
                    EXECUTE format('TRUNCATE TABLE %I.%I CASCADE', r.schemaname, r.tablename);
                END LOOP;
            END$$;
            '""";
    private DevServicesContext context;
    private Map<String, String> config = new HashMap<>();
    private Network sharedNetwork;

    @Override
    public Map<String, String> start() {
        String hostAdress = "localhost";
        try {
            hostAdress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (!initialized.get()) {
            sharedNetwork = Network.newNetwork();
            var addressPostgres = getOrStartContainerAddress(POSTGRES_SERVICE_LABEL, POSTGRES_VALUE, POSTGRES_PORT,
                    startPostgresContainer());
            postgresCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressPostgres.getId()).withCmd(DROP_TABLE);

            var addressPostgis = getOrStartContainerAddress(POSTGIS_SERVICE_LABEL, POSTGIS_VALUE, POSTGIS_PORT,
                    startPostgisContainer());
            postgisCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressPostgis.getId()).withCmd(DROP_TABLE_POSTGIS);

            var addressElastic = getOrStartContainerAddress(ELASTIC_SERVICE_LABEL, ELASTIC_VALUE, ELASTIC_PORT,
                    startElasticContainer());
            elasticCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressElastic.getId())
                    .withCmd("curl -X DELETE http://localhost:9200/_all");

            var addressKafka = getOrStartContainerAddress(KAFKA_SERVICE_LABEL, KAFKA_VALUE, KAFKA_PORT,
                    startKafkaContainer());
            kafkaCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressKafka.getId());

            var addressKeycloak = getOrStartContainerAddress(KeycloakContainer.KEYCLOAK_SERVICE_LABEL,
                    KeycloakContainer.KEYCLOAK_VALUE, KeycloakContainer.KEYCLOAK_PORT, startKeycloakContainer(hostAdress));
            keycloakCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressKeycloak.getId());

            var addressDataRef = getOrStartContainerAddress(DEV_SERVICE_LABEL, DEV_VALUE, DATA_REF_PORT,
                    startRefContainer(addressPostgres, addressKeycloak, hostAdress));
            datarefCleanUp = DockerClientFactory.lazyClient().execCreateCmd(addressDataRef.getId());

            // From the process under test, we use <addressXXX>.getPort which is resolve as 'docker/127.0.0.1' to reach running process in dind service
            config.put("provoly.virt.elasticsearch.host", addressElastic.getHost());
            config.put("provoly.virt.elasticsearch.port", String.valueOf(addressElastic.getPort()));
            config.put("provoly.virt.elasticsearch.protocol", "http");
            config.put("provoly.virt.elasticsearch.username", ELASTIC_VALUE);
            config.put("provoly.virt.elasticsearch.password", ELASTIC_VALUE);
            config.put("quarkus.rest-client.data-ref.url",
                    "http://" + addressDataRef.getHost() + ":" + addressDataRef.getPort());
            config.put("kafka.bootstrap.servers", addressKafka.getHost() + ":" + addressKafka.getPort());
            config.put("quarkus.oidc.auth-server-url",
                    "http://%s:%s/realms/provoly".formatted(hostAdress, addressKeycloak.getPort()));
            config.put("quarkus.oidc-client.auth-server-url",
                    "http://%s:%s/realms/provoly".formatted(hostAdress, addressKeycloak.getPort()));
            config.put("quarkus.rest-client.\"sso\".url", "http://%s:%s".formatted(hostAdress, addressKeycloak.getPort()));

            context.devServicesProperties().putAll(config);
            initialized.set(true);
        }
        return config;
    }

    @Override
    public void stop() {
        postgresCleanUp.exec();
        postgisCleanUp.exec();
        elasticCleanUp.exec();
        kafkaCleanUp.exec();
        keycloakCleanUp.exec();
        datarefCleanUp.exec();
    }

    private Supplier<ContainerAddress> startPostgresContainer() {
        return () -> {
            DockerImageName postgresImage = DockerImageName
                    .parse("dh2wltsh.gra7.container-registry.ovh.net/docker-mirror/library/postgres:14.1-alpine")
                    .asCompatibleSubstituteFor("postgres");
            var postgreSQLContainer = new PostgreSQLContainer<>(postgresImage)
                    .withReuse(true)
                    .withLabel(POSTGRES_SERVICE_LABEL, POSTGRES_VALUE)
                    .withUsername(USERNAME)
                    .withPassword(PASSWORD)
                    .withDatabaseName(DATABASE_NAME)
                    .withExposedPorts(POSTGRES_PORT)
                    .withNetworkAliases("db01")
                    .withNetwork(sharedNetwork);
            postgreSQLContainer.setPortBindings(List.of("%s:%s".formatted(POSTGRES_PORT, POSTGRES_PORT)));
            postgreSQLContainer.start();
            return new ContainerAddress(postgreSQLContainer.getContainerId(), "db01",
                    postgreSQLContainer.getFirstMappedPort());
        };
    }

    private Supplier<ContainerAddress> startPostgisContainer() {
        return () -> {
            DockerImageName postgisImage = DockerImageName
                    .parse("dh2wltsh.gra7.container-registry.ovh.net/docker-mirror/library/postgis:15-3.3-alpine")
                    .asCompatibleSubstituteFor("postgis");
            var postgisSQLContainer = new PostgisContainerProvider().newInstance("16-3.4")
                    .withUsername(USERNAME_POSTGIS)
                    .withPassword(PASSWORD_POSTGIS)
                    .withDatabaseName(DATABASE_NAME_POSTGIS)
                    .withReuse(true)
                    .withLabel(POSTGIS_SERVICE_LABEL, POSTGIS_VALUE)
                    .withExposedPorts(POSTGIS_PORT);

            postgisSQLContainer.setPortBindings(List.of("%s:%s".formatted(POSTGIS_PORT, POSTGRES_PORT)));
            postgisSQLContainer.start();
            return new ContainerAddress(postgisSQLContainer.getContainerId(), postgisSQLContainer.getHost(),
                    postgisSQLContainer.getFirstMappedPort());
        };
    }

    private Supplier<ContainerAddress> startKeycloakContainer(String hostAdress) {
        return () -> {
            var keycloakContainer = new KeycloakContainer(hostAdress, "dev-realm.json");
            keycloakContainer.start();
            keycloakContainer.postInit();
            return new ContainerAddress(keycloakContainer.getContainerId(), keycloakContainer.getHost(),
                    keycloakContainer.getFirstMappedPort());
        };
    }

    private Supplier<ContainerAddress> startElasticContainer() {
        return () -> {
            var elasticContainer = new GenericContainer(
                    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.4.1"))
                    .withReuse(true)
                    .withLabel(ELASTIC_SERVICE_LABEL, ELASTIC_VALUE)
                    .withExposedPorts(ELASTIC_PORT)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withClasspathResourceMapping("elasticsearch.yml",
                            "/usr/share/elasticsearch/config/elasticsearch.yml", BindMode.READ_ONLY);

            elasticContainer.setPortBindings(List.of("%s:%s".formatted(ELASTIC_PORT, ELASTIC_PORT)));
            elasticContainer
                    .setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.of(3l, ChronoUnit.MINUTES)));
            elasticContainer.start();

            return new ContainerAddress(elasticContainer.getContainerId(), elasticContainer.getHost(),
                    elasticContainer.getFirstMappedPort());
        };
    }

    private Supplier<ContainerAddress> startRefContainer(ContainerAddress addressPostgres, ContainerAddress keycloakAdress,
            String hostAdress) {
        var imagePullPolicy = imagePolicy.equals("alwaysPull") ? PullPolicy.alwaysPull() : PullPolicy.defaultPolicy();
        return () -> {
            var dataRefContainer = new GenericContainer(DockerImageName.parse(fullImageName))
                    .withReuse(true)
                    .withLabel(DEV_SERVICE_LABEL, DEV_VALUE)
                    .withNetwork(sharedNetwork)
                    .withImagePullPolicy(imagePullPolicy)
                    .withExposedPorts(DATA_REF_PORT)
                    .withLogConsumer(
                            (Consumer<OutputFrame>) outputFrame -> System.out.print("DATA_REF:" + outputFrame.getUtf8String()))
                    .withClasspathResourceMapping("application-container-data-ref.yaml", fileSystemBind, BindMode.READ_ONLY)
                    .withEnv("quarkus.datasource.db-kind", "postgresql")
                    .withEnv("quarkus.datasource.username", USERNAME)
                    .withEnv("quarkus.datasource.password", PASSWORD)
                    .withEnv("quarkus.datasource.jdbc.url", getJdbcUrl(addressPostgres))
                    .withEnv("quarkus.oidc.auth-server-url",
                            "http://%s:%s/realms/provoly".formatted(hostAdress, keycloakAdress.getPort()))
                    .withEnv("quarkus.rest-client.\"sso\".url",
                            "http://%s:%S/realms/provoly".formatted(hostAdress, keycloakAdress.getPort()))
                    .withEnv("kafka.bootstrap.servers", "kafka:29092")
                    .withNetworkAliases("localhost");
            dataRefContainer.start();
            return new ContainerAddress(dataRefContainer.getContainerId(), dataRefContainer.getHost(),
                    dataRefContainer.getFirstMappedPort());
        };
    }

    private Supplier<ContainerAddress> startKafkaContainer() {
        var kafkaTestContainer = new RedpandaTestContainer(DockerImageName.parse(KAFKA_DOCKER_IMAGE), KAFKA_PORT,
                KAFKA_SERVICE_LABEL);
        return () -> {
            var kafkaContainer = kafkaTestContainer
                    .withReuse(true)
                    .withLabel(KAFKA_SERVICE_LABEL, KAFKA_VALUE)
                    .withNetwork(sharedNetwork)
                    .withNetworkAliases("kafka")
                    .withExposedPorts(KAFKA_PORT);
            kafkaContainer.start();
            return new ContainerAddress(kafkaContainer.getContainerId(), kafkaContainer.getHost(),
                    kafkaContainer.getFirstMappedPort());
        };
    }

    private ContainerAddress getOrStartContainerAddress(String label, String value, int port,
            Supplier<ContainerAddress> containerAddressSupplier) {
        ContainerLocator containerLocator = new ContainerLocator(label, port);
        var maybeAddress = containerLocator.locateContainer(value, true, LaunchMode.DEVELOPMENT);
        return maybeAddress.orElseGet(containerAddressSupplier);
    }

    private String getJdbcUrl(ContainerAddress containerAddress) {
        return "jdbc:postgresql://db01:" + containerAddress.getPort() + "/" + DATABASE_NAME;
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }
}
