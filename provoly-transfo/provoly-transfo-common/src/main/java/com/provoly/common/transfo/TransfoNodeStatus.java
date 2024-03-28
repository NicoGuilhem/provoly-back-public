package com.provoly.common.transfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TransfoNodeStatus {

    private final UUID nodeId;
    private final IntermediateModel outModel;

    private final Set<TransfoNodeError> errors = new HashSet<>();

    public TransfoNodeStatus(UUID nodeId) {
        this(nodeId, (IntermediateModel) null);
    }

    public TransfoNodeStatus(UUID nodeId, IntermediateModel outModel) {
        this.nodeId = nodeId;
        this.outModel = outModel;
    }

    public TransfoNodeStatus(UUID nodeId, TransfoNodeError error) {
        this.nodeId = nodeId;
        this.outModel = null;
        errors.add(error);
    }

    public void addError(TransfoNodeError nodeError) {
        errors.add(nodeError);
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public IntermediateModel getOutModel() {
        return outModel;
    }

    public Collection<TransfoNodeError> getErrors() {
        return errors;
    }

}
