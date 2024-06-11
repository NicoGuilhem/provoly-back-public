package com.provoly.virt.storage.postgis;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public final class PostgisLivenessCheck extends PostgisHealthCheck {
    public PostgisLivenessCheck() {
        super("liveness check");
    }

    @Override
    public HealthCheckResponse doAdditionalCheck(HealthCheckResponseBuilder builder) {
        // no additional check for liveness
        return builder.build();
    }
}
