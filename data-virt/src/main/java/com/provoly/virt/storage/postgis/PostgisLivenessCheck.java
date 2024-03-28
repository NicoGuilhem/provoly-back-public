package com.provoly.virt.storage.postgis;

import javax.sql.DataSource;

import org.eclipse.microprofile.health.Liveness;

@Liveness
public class PostgisLivenessCheck extends PostgisHealthCheck {
    public PostgisLivenessCheck(DataSource datasource) {
        super(datasource, "liveness check");
    }
}
