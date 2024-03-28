package com.provoly.common.exec;

import java.util.UUID;

public class DatasetOutcomeDto {

    private final OutcomeMethod method;
    private final UUID datasetId;

    public DatasetOutcomeDto(OutcomeMethod method, UUID datasetId) {
        this.method = method;
        this.datasetId = datasetId;
    }

    public OutcomeMethod getMethod() {
        return method;
    }

    public UUID getDatasetId() {
        return datasetId;
    }
}
