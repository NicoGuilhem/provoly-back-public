package com.provoly.virt.storage.postgis;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.agroal.runtime.DataSourceJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public abstract sealed class PostgisHealthCheck implements HealthCheck permits PostgisLivenessCheck, PostgisReadinessCheck {

    @Inject
    DataSource datasource;

    private String message;

    @Inject
    DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig;

    protected PostgisHealthCheck(String message) {
        this.message = message;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Postgis java client health check - %s".formatted(message)).up();
        if (!isPostgisDatasourceConfigured()) {
            return builder.withData("reason", "Postgis is not configured and not required")
                    .build();
        }
        try {
            AgroalDataSourceConfiguration dataSource = ((AgroalDataSource) datasource).getConfiguration();
            if (dataSource == null) {
                return builder.down().build();
            }
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return doAdditionalCheck(builder);
    }

    private boolean isPostgisDatasourceConfigured() {
        DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig = dataSourcesJdbcRuntimeConfig
                .dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).jdbc();
        return dataSourceJdbcRuntimeConfig.url().isPresent();
    }

    public abstract HealthCheckResponse doAdditionalCheck(HealthCheckResponseBuilder builder);
}
