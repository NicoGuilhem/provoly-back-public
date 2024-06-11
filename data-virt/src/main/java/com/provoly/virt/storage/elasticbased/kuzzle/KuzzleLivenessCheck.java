package com.provoly.virt.storage.elasticbased.kuzzle;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.virt.storage.elasticbased.KuzzleClient;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class KuzzleLivenessCheck implements HealthCheck {

    private KuzzleClient kuzzleClient;

    public KuzzleLivenessCheck(KuzzleClient kuzzleClient) {
        this.kuzzleClient = kuzzleClient;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Kuzzle client - liveness check");
        if (!kuzzleClient.isConfigured()) {
            builder.withData("reason", "Kuzzle is not configured and not required");
            return builder.build();
        }
        try {
            if (kuzzleClient.client().getServerController().info().get() == null) {
                builder.down();
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
