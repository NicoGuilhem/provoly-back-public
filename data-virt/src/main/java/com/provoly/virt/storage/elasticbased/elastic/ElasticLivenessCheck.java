package com.provoly.virt.storage.elasticbased.elastic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.elasticsearch.client.RestClient;

@Liveness
@ApplicationScoped
public class ElasticLivenessCheck implements HealthCheck {

    private Instance<RestClient> restClient;

    public ElasticLivenessCheck(Instance<RestClient> restClient) {
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
        builder.name("Elasticsearch client - liveness check");
        try {
            if (!restClient.get().isRunning()) {
                builder.down();
            }

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
