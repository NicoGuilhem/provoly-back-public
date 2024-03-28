package com.provoly.virt.storage.elasticbased.kuzzle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import io.kuzzle.sdk.Kuzzle;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class KuzzleLivenessCheck implements HealthCheck {

    private Instance<Kuzzle> restClient;

    public KuzzleLivenessCheck(Instance<Kuzzle> restClient) {
        this.restClient = restClient;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        if (!restClient.isResolvable() || restClient.get() == null) {
            builder.name("Smoke Liveness - liveness check");
            builder.withData("reason", "No smoke going out");
            return builder.build();
        }
        builder.name("Kuzzle client - liveness check");
        try {
            if (restClient.get().getServerController().info().get() != null) {
                builder.down();
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
