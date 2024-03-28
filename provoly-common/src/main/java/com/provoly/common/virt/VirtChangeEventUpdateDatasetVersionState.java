package com.provoly.common.virt;

import com.provoly.common.dataset.DatasetVersionDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirtChangeEventUpdateDatasetVersionState extends VirtChangeEvent {

    protected final DatasetVersionDto datasetVersionDto;

    @JsonCreator
    public VirtChangeEventUpdateDatasetVersionState(DatasetVersionDto datasetVersionDto) {
        super(Type.UPDATE_DATASET_VERSION_STATE);
        this.datasetVersionDto = datasetVersionDto;
    }

    public DatasetVersionDto getDatasetVersionDto() {
        return datasetVersionDto;
    }
}
