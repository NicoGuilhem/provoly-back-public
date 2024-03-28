package com.provoly.common.transfo;

import java.util.Set;
import java.util.UUID;

public class TransfoErrorDatasetConflict extends TransfoError {

    private final Set<UUID> datasetIds;

    public TransfoErrorDatasetConflict(Set<UUID> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public Set<UUID> getDatasetIds() {
        return datasetIds;
    }
}
