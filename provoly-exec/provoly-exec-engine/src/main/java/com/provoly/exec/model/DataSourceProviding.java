package com.provoly.exec.model;

import java.util.UUID;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.provoly.common.exec.ProvidingMethod;

/**
 * Describe all information relative to datasource used by the job
 * Which datasource
 * Means of providing
 * ...
 */
@Embeddable
public class DataSourceProviding {

    @Enumerated(EnumType.STRING)
    private ProvidingMethod method;
    private UUID dataSourceId;

    protected DataSourceProviding() {
        // Only for JPA
    }

    public ProvidingMethod getMethod() {
        return method;
    }

    public void setMethod(ProvidingMethod method) {
        this.method = method;
    }

    public UUID getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(UUID dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

}
