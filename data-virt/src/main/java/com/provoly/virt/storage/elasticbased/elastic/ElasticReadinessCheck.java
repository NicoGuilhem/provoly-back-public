package com.provoly.virt.storage.elasticbased.elastic;

import jakarta.enterprise.context.ApplicationScoped;

import io.vertx.core.json.JsonObject;

import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

@Readiness
@ApplicationScoped
public class ElasticReadinessCheck implements HealthCheck {

    private RestClient restClient;

    public ElasticReadinessCheck(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final String STATUS = "status";

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().up();
        builder.name("Elasticsearch cluster - readiness check");
        if (restClient == null) {
            builder.withData("reason", "elasticsearch is not configured and not required");
            return builder.build();
        }
        try {
            Request request = new Request("GET", "/_cluster/health");
            Response response = restClient.performRequest(request);
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
