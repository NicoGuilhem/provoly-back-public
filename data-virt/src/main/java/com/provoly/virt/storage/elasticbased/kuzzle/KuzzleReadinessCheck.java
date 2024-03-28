package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.kuzzle.sdk.Kuzzle;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class KuzzleReadinessCheck implements HealthCheck {

    @Inject
    Instance<Kuzzle> restClient;

    private static final String STATUS = "status";

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        if (!restClient.isResolvable() || restClient.get() == null) {
            builder.name("Smoke readiness - readiness check");
            builder.withData("reason", "No smoke going out");
            return builder.build();
        }
        builder.name("Kuzzle cluster - readiness check");
        try {
            Map<String, Object> response = (Map<String, Object>) restClient.get().query(Map.of(
                    "controller", "server",
                    "action", "healthCheck")).get().getResult();

            String status = response.get(STATUS).toString();

            if ("red".equals(status)) {
                builder.down().withData(STATUS, status);
            } else {
                builder.up().withData(STATUS, status);
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
