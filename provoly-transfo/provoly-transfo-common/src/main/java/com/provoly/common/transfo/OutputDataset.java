package com.provoly.common.transfo;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OutputDataset extends NodeSpec {
    private final UUID dataset;

    @JsonCreator
    @Default
    public OutputDataset(UUID dataset) {
        super();
        this.dataset = dataset;
    }

    public UUID getDataset() {
        return dataset;
    }

    @Override
    public TransfoNodeStatus validate(UUID nodeId, IntermediateModel inModel) {
        var status = new TransfoNodeStatus(nodeId);
        if (dataset == null) {
            status.addError(new TransfoNodeErrorMissingProperty("dataset"));
        }
        return status;
    }
}
