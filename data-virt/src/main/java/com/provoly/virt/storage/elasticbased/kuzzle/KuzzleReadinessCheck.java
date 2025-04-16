package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.virt.storage.elasticbased.KuzzleClient;

import io.kuzzle.sdk.protocol.ProtocolState;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

@Readiness
@ApplicationScoped
public class KuzzleReadinessCheck implements HealthCheck {

    private static final Map<String, String> QUERY_CHECK = Map.of(
            "controller", "server",
            "action", "healthCheck");
    private static final String STATUS = "status";

    private final Logger log;
    private final KuzzleClient kuzzleClient;

    public KuzzleReadinessCheck(Logger log, KuzzleClient kuzzleClient) {
        this.log = log;
        this.kuzzleClient = kuzzleClient;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Kuzzle cluster - readiness check");
        if (!kuzzleClient.isConfigured()) {
            builder.withData("reason", "Kuzzle is not configured and not required");
            return builder.build();
        }

        if (!kuzzleConnected()) {
            builder.down().withData(STATUS, "Kuzzle is not connected");
        } else if (kuzzleReady()) {
            builder.up().withData(STATUS, "Kuzzle is connected and ready");
        } else {
            builder.down().withData(STATUS, "Kuzzle is connected but not ready");
        }
        return builder.build();
    }

    private boolean kuzzleConnected() {
        var status = kuzzleClient.getState();
        return status == ProtocolState.OPEN;
    }

    private boolean kuzzleReady() {
        try {
            log.debug("Checking kuzzle ready");
            var response = (Map<String, Object>) kuzzleClient.client()
                    .query(QUERY_CHECK)
                    .get(15, TimeUnit.SECONDS)
                    .getResult();

            String status = response.get(STATUS).toString();

            return !"red".equals(status);

        } catch (Exception e) {
            log.error("Checking kuzzle ready thrown ", e);
            return false;
        }
    }
}
