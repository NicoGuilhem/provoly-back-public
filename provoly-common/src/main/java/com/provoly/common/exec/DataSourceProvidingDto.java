package com.provoly.common.exec;

import java.util.UUID;

public class DataSourceProvidingDto {

    private final ProvidingMethod method;
    private final UUID dataSourceId;

    public DataSourceProvidingDto(ProvidingMethod method, UUID dataSourceId) {
        this.method = method;
        this.dataSourceId = dataSourceId;
    }

    @Override
    public String toString() {
        return "DataSourceProvidingDto{" +
                "method=" + method +
                ",dataSourceId=" + dataSourceId +
                '}';
    }

    public ProvidingMethod getMethod() {
        return method;
    }

    public UUID getDataSourceId() {
        return dataSourceId;
    }

}
