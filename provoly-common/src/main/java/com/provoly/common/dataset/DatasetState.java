package com.provoly.common.dataset;

import java.util.List;

public enum DatasetState {
    LOADING,
    INDEXING,
    ACTIVE,
    ERROR,
    INACTIVE,
    DELETE_ERROR,
    DELETING;

    private List<DatasetState> availableTransitions(DatasetState wantedState) {
        return switch (wantedState) {
            case ERROR, DELETE_ERROR -> List.of(DELETING);
            case LOADING -> List.of(ERROR, INDEXING);
            case INDEXING -> List.of(ERROR, ACTIVE);
            case ACTIVE -> List.of(INACTIVE, DELETING);
            case INACTIVE -> List.of(ACTIVE, DELETING);
            case DELETING -> List.of(DELETE_ERROR);
        };
    }

    public boolean canUpdateTo(DatasetState wantedState) {
        return availableTransitions(this).contains(wantedState);
    }
}
