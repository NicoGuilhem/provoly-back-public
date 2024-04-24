package kafka;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_DELETE;
import static org.apache.kafka.common.config.TopicConfig.RETENTION_BYTES_CONFIG;
import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

import java.util.*;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.smallrye.common.annotation.Identifier;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaTools {

    public static final String STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    private Logger log;

    private Map<String, Object> kafkaConfig;
    private static final Map<String, String> TOPIC_CONFIG = Map.of(
            CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE,
            RETENTION_BYTES_CONFIG, "-1",
            RETENTION_MS_CONFIG, "-1");
    private final KafkaStreamsRuntimeConfig streamConfig;
    private final Optional<String> kafkaSecurityProtocol;
    private final Optional<String> kafkaSaslMechanism;
    private final Optional<String> kafkaSaslJassConfig;
    private final Optional<String> kafkaSaslLoginCallbackHandlerClass;
    private final Set<String> alreadyExistsTopic = new HashSet<>();
    private static final String ERROR_CONNECTION_KAFKA = "Unable to connect to kafka broker";

    public KafkaTools(
            Logger log,
            @Identifier("default-kafka-broker") Map<String, Object> kafkaConfig,
            KafkaStreamsRuntimeConfig streamConfig,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.security.protocol") Optional<String> kafkaSecurityProtocol,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.sasl.mechanism") Optional<String> kafkaSaslMechanism,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.sasl.jaas.config") Optional<String> kafkaSaslJassConfig,
            @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.sasl.login.callback.handler.class") Optional<String> kafkaSaslLoginCallbackHandlerClass) {
        this.log = log;
        this.kafkaConfig = kafkaConfig;
        this.streamConfig = streamConfig;

        this.kafkaSecurityProtocol = kafkaSecurityProtocol;
        this.kafkaSaslMechanism = kafkaSaslMechanism;
        this.kafkaSaslJassConfig = kafkaSaslJassConfig;
        this.kafkaSaslLoginCallbackHandlerClass = kafkaSaslLoginCallbackHandlerClass;
        addSecurityProperties();
    }

    public Admin getAdmin() { // TODO : Check if we cannot use an instance provider
        log.info("Creating a new kafka admin");
        var props = getDefaultProperties(AdminClientConfig.configNames());
        return Admin.create(props);
    }

    @Produces
    KafkaProducer getProducer() {
        return getProducer(StringSerializer.class);
    }

    public KafkaProducer getProducer(Class<? extends Serializer> serializerClass) {
        log.info("Creating a new kafka producer");
        Map<String, Object> props = getProducerProperties();
        props.put("key.serializer", STRING_SERIALIZER);
        props.put("value.serializer", serializerClass.getCanonicalName());
        return new KafkaProducer(props);
    }

    public Map<String, Object> getProducerProperties() {
        log.info("Get default kafka producer properties");
        return getDefaultProperties(ProducerConfig.configNames());
    }

    public synchronized boolean isTopicsExists(String... names) {
        try (var client = getAdmin()) {
            return client.listTopics().names().get().containsAll(Arrays.asList(names));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERROR_CONNECTION_KAFKA, e);
        }
    }

    public KafkaStreams buildStream(String id, Topology topology) {
        return new KafkaStreams(topology, buildStreamProperties(id));
    }

    public Properties buildStreamProperties(String streamId) {
        Properties streamsProperties = new Properties();
        streamsProperties.putAll(kafkaConfig);
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, streamConfig.applicationId + "-" + streamId);
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return streamsProperties;
    }

    private Map<String, Object> getDefaultProperties(Set<String> configNames) {
        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, Object> entry : kafkaConfig.entrySet()) {
            boolean keepConfigEntry = configNames.contains(entry.getKey());
            var logPrefix = keepConfigEntry ? "using" : "skipping";
            log.debugf("%s configuration entry : %s: %s", logPrefix, entry.getKey(), entry.getValue());
            if (keepConfigEntry) {
                props.put(entry.getKey(), entry.getValue());
            }
        }
        return props;
    }

    private void addSecurityProperties() {
        streamConfig.securityProtocol.ifPresent(value -> kafkaConfig.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, value));
        streamConfig.sasl.mechanism.ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_MECHANISM, value));
        streamConfig.sasl.jaasConfig.ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_JAAS_CONFIG, value));
        streamConfig.sasl.loginCallbackHandlerClass
                .ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, value));

        kafkaSecurityProtocol
                .ifPresent(value -> kafkaConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, value));
        kafkaSaslMechanism.ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_MECHANISM, value));
        kafkaSaslJassConfig.ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_JAAS_CONFIG, value));
        kafkaSaslLoginCallbackHandlerClass
                .ifPresent(value -> kafkaConfig.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, value));
    }

}