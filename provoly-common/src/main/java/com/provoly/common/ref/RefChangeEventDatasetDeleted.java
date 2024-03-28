package com.provoly.common.ref;

import java.util.List;
import java.util.UUID;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.model.OClassDetailsDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventDatasetDeleted extends RefChangeEvent {
    private final List<DatasetVersionDto> datasetVersionDtos;
    private final OClassDetailsDto oClassDetailsDto;
    private final UUID datasetId;

    @JsonCreator
    public RefChangeEventDatasetDeleted(List<DatasetVersionDto> datasetVersionDtos, OClassDetailsDto oClassDetailsDto,
            UUID datasetId) {
        super(RefChangeEvent.Type.DATASET_DELETED);
        this.datasetVersionDtos = datasetVersionDtos;
        this.oClassDetailsDto = oClassDetailsDto;
        this.datasetId = datasetId;
    }

    @Override
    public String toString() {
        return "RefChangeEventDatasetDeleted{" +
                "datasetVersionDtos='" + datasetVersionDtos + '\'' +
                "oClassDetailsDto='" + oClassDetailsDto + '\'' +
                "} " + super.toString();
    }

    public List<DatasetVersionDto> getDatasetVersionDtos() {
        return datasetVersionDtos;
    }

    public OClassDetailsDto getoClassDetailsDto() {
        return oClassDetailsDto;
    }

    public UUID getDatasetId() {
        return datasetId;
    }
}
