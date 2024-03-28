package com.provoly.common.transfo;

import java.util.UUID;

public class NoOp extends NodeSpec {
    public NoOp() {
        super();
    }

    @Override
    public TransfoNodeStatus validate(UUID nodeId, IntermediateModel inModel) {
        return new TransfoNodeStatus(nodeId, inModel);
    }

}
