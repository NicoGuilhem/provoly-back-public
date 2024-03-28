package com.provoly.virt.storage.postgis;

import javax.sql.DataSource;

import org.eclipse.microprofile.health.Readiness;

@Readiness
public class PostgisReadinessCheck extends PostgisHealthCheck {

    public PostgisReadinessCheck(DataSource datasource) {
        super(datasource, "readiness check");
    }
}
