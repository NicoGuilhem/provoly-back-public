package com.provoly.virt.storage.elasticbased.elastic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.vertx.core.json.JsonObject;

import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.health.*;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

@Readiness
@ApplicationScoped
public class ElasticReadinessCheck implements HealthCheck {

    @Inject
    Instance<RestClient> restClient;

    private static final String STATUS = "status";

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        if (!restClient.isResolvable() || restClient.get() == null) {
            builder.name("Smoke readiness - readiness check");
            builder.withData("reason", "No smoke going out");
            return builder.build();
        }
        builder.name("Elasticsearch cluster - readiness check");
        try {
            Request request = new Request("GET", "/_cluster/health");
            Response response = restClient.get().performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = new JsonObject(responseBody);
            String status = json.getString(STATUS);
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
