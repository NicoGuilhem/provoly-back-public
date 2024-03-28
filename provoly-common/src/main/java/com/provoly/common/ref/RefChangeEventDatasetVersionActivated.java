package com.provoly.common.ref;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventDatasetVersionActivated extends RefChangeEvent {

    private final UUID datasetId;
    private final UUID datasetVersionId;

    @JsonCreator
    public RefChangeEventDatasetVersionActivated(UUID datasetId, UUID datasetVersionId) {
        super(Type.DATASET_VERSION_ACTIVATED);
        this.datasetId = datasetId;
        this.datasetVersionId = datasetVersionId;
    }

    @Override
    public String toString() {
        return "RefChangeEventDatasetActivated{" +
                "datasetId='" + datasetId + '\'' +
                "datasetVersionId='" + datasetVersionId + '\'' +
                "} " + super.toString();
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public UUID getDatasetVersionId() {
        return datasetVersionId;
    }
}
