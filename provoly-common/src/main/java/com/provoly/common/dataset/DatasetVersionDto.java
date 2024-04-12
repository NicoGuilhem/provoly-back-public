package com.provoly.common.dataset;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DatasetVersionDto {

    private UUID id;
    private UUID dataset;
    private UUID oClass;
    private Instant lastModified;
    private Integer version;
    private DatasetState state;
    private boolean withFile;
    private Instant productionDate;
    private String producer;
    private String additionalInformation;

    @Default
    @JsonCreator
    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, boolean withFile, Instant productionDate, String producer, String additionalInformation) {
        this.id = id;
        this.dataset = dataset;
        this.oClass = oClass;
        this.lastModified = lastModified;
        this.version = version;
        this.state = state;
        this.withFile = withFile;
        this.productionDate = productionDate;
        this.producer = producer;
        this.additionalInformation = additionalInformation;
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, boolean withFile) {
        this(id, dataset, oClass, lastModified, version, state, withFile, null, null, null);
    }

    public DatasetVersionDto(UUID id) {
        this(id, null, null, null, null, null, false, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, String producer, Instant productionDate) {
        this(id, dataset, null, null, null, null, false, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass) {
        this(id, dataset, oClass, null, null, null, false, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Integer version, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, version, null, false, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state) {
        this(id, dataset, oClass, null, null, state, false, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state, String producer, Instant productionDate,
            String additionalInformation) {
        this(id, dataset, oClass, null, null, state, false, productionDate, producer, additionalInformation);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            DatasetVersionInformationDto datasetVersionInformationDto) {
        this(id, dataset, oClass, null, null, state, false, datasetVersionInformationDto.productionDate(),
                datasetVersionInformationDto.producer(), datasetVersionInformationDto.additionalInformation());
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, null, state, false, productionDate, producer, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, DatasetState state) {
        this(id, dataset, null, null, null, state, false, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            boolean withFile, Instant productionDate, String producer, String additionalInformation) {
        this(id, dataset, oClass, null, null, state, withFile, productionDate, producer, additionalInformation);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            boolean withFile, DatasetVersionInformationDto datasetVersionInformationDto) {
        this(id, dataset, oClass, null, null, state, withFile, datasetVersionInformationDto.productionDate(),
                datasetVersionInformationDto.producer(), datasetVersionInformationDto.additionalInformation());
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            boolean withFile) {
        this(id, dataset, oClass, null, null, state, withFile, null, null, null);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            boolean withFile, String producer, Instant productionDate) {
        this(id, dataset, oClass, null, null, state, withFile, productionDate, producer, null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getDataset() {
        return dataset;
    }

    public UUID getoClass() {
        return oClass;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public Integer getVersion() {
        return version;
    }

    public DatasetState getState() {
        return state;
    }

    public boolean isWithFile() {
        return withFile;
    }

    public Instant getProductionDate() {
        return productionDate;
    }

    public String getProducer() {
        return producer;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

}
