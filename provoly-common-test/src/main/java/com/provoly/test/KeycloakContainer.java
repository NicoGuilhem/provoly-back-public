package com.provoly.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.ws.rs.core.HttpHeaders;

import io.quarkus.oidc.runtime.devui.OidcDevServicesUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final Logger LOG = Logger.getLogger(KeycloakContainer.class);

    public static final String KEYCLOAK_SERVICE_LABEL = "quarkus-dev-service-keycloak";
    public static final String KEYCLOAK_VALUE = "keycloak";
    public static final int KEYCLOAK_PORT = 8080;

    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";

    // Properties recognized by Quarkus-powered Keycloak
    private static final String KEYCLOAK_QUARKUS_HOSTNAME = "KC_HOSTNAME";
    private static final String KEYCLOAK_QUARKUS_PORT = "KC_HOSTNAME_PORT";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PROP = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP = "KEYCLOAK_ADMIN_PASSWORD";

    private static final String KEYCLOAK_DOCKER_IMAGE = "quay.io/keycloak/keycloak:22.0.5";
    private final String realmPath;
    private RealmRepresentation realmRep;

    public KeycloakContainer(String realmPath) {
        super(DockerImageName.parse(KEYCLOAK_DOCKER_IMAGE));
        this.realmPath = realmPath;

        super.setWaitStrategy(Wait.forLogMessage(".*Keycloak.*started.*", 1));
    }

    @Override
    protected void configure() {
        super.configure();
        // Override Hostname and port in order to have a correct iss in generated token
        addEnv(KEYCLOAK_QUARKUS_HOSTNAME, "sso");
        addEnv(KEYCLOAK_QUARKUS_PORT, String.valueOf(KEYCLOAK_PORT));
        addExposedPort(KEYCLOAK_PORT);
        withLabel(KEYCLOAK_SERVICE_LABEL, KEYCLOAK_VALUE);
        addEnv(KEYCLOAK_QUARKUS_ADMIN_PROP, KEYCLOAK_ADMIN_USER);
        addEnv(KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
        withCommand("start --http-enabled=true --hostname-strict=false --hostname-strict-https=false");

        URL realmPathUrl = null;
        if ((realmPathUrl = Thread.currentThread().getContextClassLoader().getResource(realmPath)) != null) {
            readRealmFile(realmPathUrl, realmPath).ifPresent(realmRep -> this.realmRep = realmRep);
        } else {
            Path filePath = Paths.get(realmPath);
            if (Files.exists(filePath)) {
                readRealmFile(filePath.toUri(), realmPath).ifPresent(realmRep -> this.realmRep = realmRep);
            } else {
                LOG.debugf("Realm %s resource is not available", realmPath);
            }
        }
        super.withLogConsumer(t -> {
            LOG.tracef("Keycloak: %s", t.getUtf8String());
        });

        LOG.infof("Powered Keycloak distribution Quarkus");
    }

    private Optional<RealmRepresentation> readRealmFile(URI uri, String realmPath) {
        try {
            return readRealmFile(uri.toURL(), realmPath);
        } catch (MalformedURLException ex) {
            // Will not happen as this method is called only when it is confirmed the file exists
            throw new RuntimeException(ex);
        }
    }

    private Optional<RealmRepresentation> readRealmFile(URL url, String realmPath) {
        try {
            try (InputStream is = url.openStream()) {
                return Optional.of(JsonSerialization.readValue(is, RealmRepresentation.class));
            }
        } catch (IOException ex) {
            LOG.errorf("Realm %s resource can not be opened: %s", realmPath, ex.getMessage());
        }
        return Optional.empty();
    }

    public String getAdminToken(WebClient client, String keycloakUrl) {
        try {
            LOG.tracef("Acquiring admin token");

            return OidcDevServicesUtils.getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", null)
                    .await().atMost(Duration.ofSeconds(45));
        } catch (Throwable t) {
            LOG.error("Admin token can not be acquired", t);
        }
        return null;
    }

    private void createRealm(WebClient client, String token, String keycloakUrl, RealmRepresentation realm) {
        try {
            LOG.tracef("Creating the realm %s", realm.getRealm());
            HttpResponse<Buffer> createRealmResponse = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(Duration.ofSeconds(45));

            if (createRealmResponse.statusCode() > 299) {
                LOG.errorf("Realm %s can not be created %d - %s ", realm.getRealm(), createRealmResponse.statusCode(),
                        createRealmResponse.statusMessage());
            }

            Uni<Integer> realmStatusCodeUni = client.getAbs(keycloakUrl + "/realms/" + realm.getRealm())
                    .send().onItem()
                    .transform(resp -> {
                        LOG.debugf("Realm status: %d", resp.statusCode());
                        if (resp.statusCode() == 200) {
                            LOG.debugf("Realm %s has been created", realm.getRealm());
                            return 200;
                        } else {
                            throw new RealmEndpointAccessException(resp.statusCode());
                        }
                    }).onFailure(realmEndpointNotAvailable())
                    .retry()
                    .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(2))
                    .expireIn(10 * 1000)
                    .onFailure().transform(t -> {
                        return new RuntimeException("Keycloak server is not available"
                                + (t.getMessage() != null ? (": " + t.getMessage()) : ""));
                    });
            realmStatusCodeUni.await().atMost(Duration.ofSeconds(10));
        } catch (Throwable t) {
            LOG.errorf(t, "Realm %s can not be created", realm.getRealm());
        }
    }

    public void postInit() {
        var vertxInstance = Vertx.vertx();
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        String clientAuthServerBaseUrl = "http://127.0.0.1:%s".formatted(getFirstMappedPort());
        try {
            String adminToken = getAdminToken(client, clientAuthServerBaseUrl);
            if (realmRep != null) {
                createRealm(client, adminToken, clientAuthServerBaseUrl, realmRep);
            }
        } finally {
            client.close();
        }
    }

    @SuppressWarnings("serial")
    static class RealmEndpointAccessException extends RuntimeException {
        private final int errorStatus;

        public RealmEndpointAccessException(int errorStatus) {
            this.errorStatus = errorStatus;
        }

        public int getErrorStatus() {
            return errorStatus;
        }
    }

    public static Predicate<? super Throwable> realmEndpointNotAvailable() {
        return t -> (t instanceof ConnectException
                || (t instanceof RealmEndpointAccessException && ((RealmEndpointAccessException) t).getErrorStatus() == 404));
    }
}
