package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.virt.storage.elasticbased.KuzzleClient;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class KuzzleReadinessCheck implements HealthCheck {

    @Inject
    private KuzzleClient kuzzleClient;

    private static final String STATUS = "status";

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Kuzzle cluster - readiness check");
        if (!kuzzleClient.isConfigured()) {
            builder.withData("reason", "Kuzzle is not configured and not required");
            return builder.build();
        }
        try {
            Map<String, Object> response = (Map<String, Object>) kuzzleClient.client().query(Map.of(
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
