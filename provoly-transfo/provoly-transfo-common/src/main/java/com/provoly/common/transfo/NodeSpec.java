package com.provoly.common.transfo;

import java.util.UUID;

public abstract class NodeSpec {

    public abstract TransfoNodeStatus validate(UUID nodeId, IntermediateModel inModel);

    protected boolean needInput() {
        return true;
    }

}
