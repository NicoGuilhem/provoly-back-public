package com.provoly.common.virt;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.imports.ImportsMessage;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirtChangeEventUpdateDatasetVersionStateAndMessageCreated extends VirtChangeEvent {
    protected final DatasetVersionDto datasetVersionDto;
    protected final ImportsMessage importsMessage;

    @JsonCreator
    public VirtChangeEventUpdateDatasetVersionStateAndMessageCreated(DatasetVersionDto datasetVersionDto,
            ImportsMessage importsMessage) {
        super(Type.UPDATE_DATASET_VERSION_STATE_AND_IMPORT_MESSAGE);
        this.datasetVersionDto = datasetVersionDto;
        this.importsMessage = importsMessage;
    }

    public DatasetVersionDto getDatasetVersionDto() {
        return datasetVersionDto;
    }

    public ImportsMessage getImportsMessage() {
        return importsMessage;
    }
}
