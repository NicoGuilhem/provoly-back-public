package com.provoly.common.virt;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirtChangeEventDeleteDatasetVersionError extends VirtChangeEvent {
    protected UUID datasetVersionId;

    @JsonCreator
    public VirtChangeEventDeleteDatasetVersionError(UUID datasetVersionId) {
        super(Type.DELETE_DATASET_VERSION_ERROR);
        this.datasetVersionId = datasetVersionId;
    }

    public UUID getDatasetVersionId() {
        return datasetVersionId;
    }

}
