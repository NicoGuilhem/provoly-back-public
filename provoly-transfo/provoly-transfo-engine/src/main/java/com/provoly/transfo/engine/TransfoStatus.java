package com.provoly.transfo.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.provoly.common.transfo.TransfoError;
import com.provoly.common.transfo.TransfoNodeStatus;

public class TransfoStatus {

    private final Collection<TransfoError> errors = new ArrayList<>();
    private final Collection<TransfoNodeStatus> status = new ArrayList<>();

    public TransfoNodeStatus getNodeStatus(UUID id) {
        return status.stream().filter(n -> n.getNodeId().equals(id)).findAny().orElseThrow();
    }

    public void addError(TransfoError error) {
        errors.add(error);
    }

    public void add(Collection<TransfoNodeStatus> values) {
        status.addAll(values);
    }

    public void add(TransfoNodeStatus nodeStatus) {
        status.add(nodeStatus);
    }

    public Collection<TransfoError> getErrors() {
        return errors;
    }

    public Collection<TransfoNodeStatus> getStatus() {
        return status;
    }

}
