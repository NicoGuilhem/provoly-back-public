package com.provoly.common.virt;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirtChangeEventDatasetVersionDeleted extends VirtChangeEvent {
    protected UUID datasetVersionId;

    @JsonCreator
    public VirtChangeEventDatasetVersionDeleted(UUID datasetVersionId) {
        super(Type.DELETED_DATASET_VERSION);
        this.datasetVersionId = datasetVersionId;
    }

    public UUID getDatasetVersionId() {
        return datasetVersionId;
    }
}
