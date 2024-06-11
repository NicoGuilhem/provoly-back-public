package com.provoly.virt.storage.postgis;

import java.sql.Connection;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public final class PostgisReadinessCheck extends PostgisHealthCheck {

    public PostgisReadinessCheck() {
        super("readiness check");
    }

    private HealthCheckResponse testConnexion(HealthCheckResponseBuilder builder) {
        try (Connection connexion = this.datasource.getConnection()) {
            boolean valid = connexion.isValid(1);
            if (!valid) {
                builder.down().withData("status", "DOWN");
            } else {
                builder.withData("status", "UP");
            }
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }

    @Override
    public HealthCheckResponse doAdditionalCheck(HealthCheckResponseBuilder builder) {
        return testConnexion(builder);
    }
}
