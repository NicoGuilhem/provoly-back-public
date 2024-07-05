package com.provoly.common.dataset;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public non-sealed class DatasetVersionDto extends DatasetVersionBaseDto {

    private UUID dataset;

    @Default
    @JsonCreator
    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, String fileName, Instant productionDate, String producer, String additionalInformation) {
        super(id, oClass, lastModified, version,
                state, fileName, productionDate, producer, additionalInformation);
        this.dataset = dataset;
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, String fileName) {
        this(id, dataset, oClass, lastModified, version, state, fileName, null, null, null);
    }

    public DatasetVersionDto(UUID id) {
        this(id, null, null, null, null, null, null, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, String producer, Instant productionDate) {
        this(id, dataset, null, null, null, null, null, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass) {
        this(id, dataset, oClass, null, null, null, null, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Integer version, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, version, null, null, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state) {
        this(id, dataset, oClass, null, null, state, null, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state, String producer, Instant productionDate,
            String additionalInformation) {
        this(id, dataset, oClass, null, null, state, null, productionDate, producer, additionalInformation);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            DatasetVersionInformationDto datasetVersionInformationDto) {
        this(id, dataset, oClass, null, null, state, null, datasetVersionInformationDto.productionDate(),
                datasetVersionInformationDto.producer(), datasetVersionInformationDto.additionalInformation());
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, null, state, null, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, DatasetState state) {
        this(id, dataset, null, null, null, state, null, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            String fileName, Instant productionDate, String producer, String additionalInformation) {
        this(id, dataset, oClass, null, null, state, fileName, productionDate, producer, additionalInformation);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            String fileName, DatasetVersionInformationDto datasetVersionInformationDto) {
        this(id, dataset, oClass, null, null, state, fileName, datasetVersionInformationDto.productionDate(),
                datasetVersionInformationDto.producer(), datasetVersionInformationDto.additionalInformation());
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            String fileName) {
        this(id, dataset, oClass, null, null, state, fileName, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            String fileName, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, null, state, fileName, productionDate, producer, null);
    }

    public UUID getDataset() {
        return dataset;
    }

    public void setDataset(UUID datasetId) {
        this.dataset = datasetId;
    }
}
