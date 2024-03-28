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

    @Default
    @JsonCreator
    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Instant lastModified, Integer version,
            DatasetState state, boolean withFile) {
        this.id = id;
        this.dataset = dataset;
        this.oClass = oClass;
        this.lastModified = lastModified;
        this.version = version;
        this.state = state;
        this.withFile = withFile;
    }

    public DatasetVersionDto(UUID id) {
        this(id, null, null, null, null, null, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset) {
        this(id, dataset, null, null, null, null, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass) {
        this(id, dataset, oClass, null, null, null, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, Integer version) {
        this(id, dataset, oClass, null, version, null, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state) {
        this(id, dataset, oClass, null, null, state, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset, DatasetState state) {
        this(id, dataset, null, null, null, state, false);
    }

    public DatasetVersionDto(UUID id, UUID dataset, UUID oClass, DatasetState state,
            boolean withFile) {
        this(id, dataset, oClass, null, null, state, withFile);
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
}
