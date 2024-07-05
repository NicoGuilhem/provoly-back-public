package com.provoly.common.dataset;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract sealed class DatasetVersionBaseDto permits DatasetVersionDto, DatasetVersionDetailsDto {

    private UUID id;
    private UUID oClass;
    private Instant lastModified;
    private Integer version;
    private DatasetState state;
    private String fileName;
    private Instant productionDate;
    private String producer;
    private String additionalInformation;

    @Default
    @JsonCreator
    protected DatasetVersionBaseDto(UUID id, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, String fileName, Instant productionDate, String producer, String additionalInformation) {
        this.id = id;
        this.oClass = oClass;
        this.lastModified = lastModified;
        this.version = version;
        this.state = state;
        this.fileName = fileName;
        this.productionDate = productionDate;
        this.producer = producer;
        this.additionalInformation = additionalInformation;
    }

    public UUID getId() {
        return id;
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

    public String getFileName() {
        return fileName;
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

    /**
     * withFile is not exported as fileName is present
     *
     * @see DatasetVersionBaseDto#getFileName()
     */
    @JsonIgnore
    public boolean isWithFile() {
        return getFileName() != null && !getFileName().isBlank();
    }

    public void setoClass(UUID oClass) {
        this.oClass = oClass;
    }
}
