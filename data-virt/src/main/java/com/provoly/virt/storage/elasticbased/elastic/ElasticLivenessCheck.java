package com.provoly.virt.storage.elasticbased.elastic;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.elasticsearch.client.RestClient;

@Liveness
@ApplicationScoped
public class ElasticLivenessCheck implements HealthCheck {

    private RestClient restClient;

    public ElasticLivenessCheck(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Elasticsearch client - liveness check");
        if (restClient == null) {
            builder.withData("reason", "elasticsearch is not configured and not required");
            return builder.build();
        }
        try {
            if (!restClient.isRunning()) {
                builder.down();
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
