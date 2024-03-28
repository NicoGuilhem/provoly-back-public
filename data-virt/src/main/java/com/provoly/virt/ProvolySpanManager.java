package com.provoly.virt;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/*
* Service that generates intermediate spans for additional observability metrics.
* The span is not closed if an exception is thrown.
* See https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/exceptions.md#recording-an-exception
 * */
@ApplicationScoped
public class ProvolySpanManager {

    private Tracer tracer;

    public ProvolySpanManager(Tracer tracer) {
        this.tracer = tracer;
    }

    public Span generateSpan(String label, Map<String, String> tags) {
        var builder = tracer.spanBuilder(label)
                .setParent(Context.current().with(Span.current()))
                .setSpanKind(SpanKind.INTERNAL);

        tags.forEach(builder::setAttribute);
        return builder.startSpan();

    }

    public void recordException(Span span, Exception e) {
        span.recordException(e, Attributes.of(AttributeKey.booleanKey("exception.escaped"), true));
    }
}
