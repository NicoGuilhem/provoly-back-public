package com.provoly.virt.storage.postgis;

import java.sql.Connection;
import javax.sql.DataSource;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public class PostgisHealthCheck implements HealthCheck {
    private DataSource datasource;
    private String message;

    protected PostgisHealthCheck(DataSource dataSource, String message) {
        this.datasource = dataSource;
        this.message = message;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Postgis java client health check - %s".formatted(message)).up();
        if (datasource instanceof UnconfiguredDataSource) {
            return builder.name("Smoke Liveness - %s".formatted(message))
                    .withData("reason", "No smoke going out")
                    .build();
        }
        try {
            AgroalDataSourceConfiguration dataSource = ((AgroalDataSource) datasource).getConfiguration();
            if (dataSource != null) {
                builder.up();
            } else {
                return builder.down().build();
            }
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return testConnexion(builder);
    }

    public HealthCheckResponse testConnexion(HealthCheckResponseBuilder builder) {
        try (Connection connexion = datasource.getConnection()) {
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
}
