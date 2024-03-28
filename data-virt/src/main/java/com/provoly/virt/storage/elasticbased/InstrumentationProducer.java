package com.provoly.virt.storage.elasticbased;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.opentelemetry.api.OpenTelemetry;

import co.elastic.clients.transport.instrumentation.Instrumentation;
import co.elastic.clients.transport.instrumentation.OpenTelemetryForElasticsearch;

@ApplicationScoped
public class InstrumentationProducer {
    private OpenTelemetry openTelemetry;

    public InstrumentationProducer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Produces
    public Instrumentation getInstrumentation() {
        return new OpenTelemetryForElasticsearch(openTelemetry, true);
    }
}
