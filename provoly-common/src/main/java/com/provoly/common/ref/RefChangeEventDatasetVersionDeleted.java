package com.provoly.common.ref;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventDatasetVersionDeleted extends RefChangeEvent {
    private final DatasetVersionDto datasetVersionDto;
    private final OClassDetailsDto oClassDetailsDto;

    @JsonCreator
    public RefChangeEventDatasetVersionDeleted(DatasetVersionDto datasetVersionDto, OClassDetailsDto oClassDetailsDto) {
        super(Type.DATASET_VERSION_DELETED);
        this.datasetVersionDto = datasetVersionDto;
        this.oClassDetailsDto = oClassDetailsDto;
    }

    @Override
    public String toString() {
        return "RefChangeEventDatasetVersionDeleted{" +
                "datasetVersionDto='" + datasetVersionDto + '\'' +
                "oClassDetailsDto='" + oClassDetailsDto + '\'' +
                "} " + super.toString();
    }

    public DatasetVersionDto getDatasetVersionDto() {
        return datasetVersionDto;
    }

    public OClassDetailsDto getoClassDetailsDto() {
        return oClassDetailsDto;
    }
}
