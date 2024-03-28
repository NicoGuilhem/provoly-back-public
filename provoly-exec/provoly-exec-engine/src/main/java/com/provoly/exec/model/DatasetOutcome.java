package com.provoly.exec.model;

import java.util.UUID;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.provoly.common.exec.OutcomeMethod;

@Embeddable
public class DatasetOutcome {

    @Enumerated(EnumType.STRING)
    private OutcomeMethod method;
    private UUID datasetId;

    public OutcomeMethod getMethod() {
        return method;
    }

    public void setMethod(OutcomeMethod method) {
        this.method = method;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }
}
