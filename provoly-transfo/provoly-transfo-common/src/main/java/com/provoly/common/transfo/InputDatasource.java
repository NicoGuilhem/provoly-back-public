package com.provoly.common.transfo;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

// Warn : Should be InputDatasource, but frontend use InputDatasource as type name
public class InputDatasource extends NodeSpec {
    private final UUID datasetId;

    @JsonCreator
    @Default
    public InputDatasource(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    @Override
    public TransfoNodeStatus validate(UUID id, IntermediateModel inModel) {
        return null;
    }
}
