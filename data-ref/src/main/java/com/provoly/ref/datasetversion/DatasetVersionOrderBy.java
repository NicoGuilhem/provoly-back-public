package com.provoly.ref.datasetversion;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;

public enum DatasetVersionOrderBy {
    DATASET_NAME((datasetVersion, dataset) -> dataset.get(Dataset_.name)),
    DATE((datasetVersion, dataset) -> datasetVersion.get(DatasetVersion_.lastModified));

    public static final DatasetVersionOrderBy DEFAULT = DatasetVersionOrderBy.DATE;

    private final PathAssociation associatedModelFunction;

    DatasetVersionOrderBy(PathAssociation associatedModelFunction) {
        this.associatedModelFunction = associatedModelFunction;
    }

    public Path getCriteriaPath(Root<DatasetVersion> datasetVersion, From<DatasetVersion, Dataset> dataSet) {
        return associatedModelFunction.getPathForOrderBy(datasetVersion, dataSet);
    }

    @FunctionalInterface
    private interface PathAssociation {
        public Path getPathForOrderBy(Root<DatasetVersion> datasetVersion, From<DatasetVersion, Dataset> dataSet);
    }
}
