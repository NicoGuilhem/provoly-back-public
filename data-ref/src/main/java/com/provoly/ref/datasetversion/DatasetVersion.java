package com.provoly.ref.datasetversion;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.common.dataset.DatasetState;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.entity.EntityId;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class DatasetVersion extends EntityId {
    @UpdateTimestamp
    private Instant lastModified;

    private String fileName;

    @ManyToOne
    @Immutable
    private Dataset dataset;

    private Integer version;

    @Enumerated(EnumType.STRING)
    private DatasetState state;

    private Instant productionDate;
    private String producer;
    private String additionalInformation;

    protected DatasetVersion() {
        super();
    }

    @Default
    public DatasetVersion(UUID id) {
        super(id);
    }

    @Override
    public String toString() {
        return "DatasetVersion{" +
                "id=" + id +
                ", version=" + version +
                ", lastModified=" + lastModified +
                ", dataset=" + dataset.getId() +
                ", state=" + state +
                ", fileName=" + fileName +
                "} ";
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public DatasetState getState() {
        return state;
    }

    public void setState(DatasetState state) {
        this.state = state;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Instant getProductionDate() {
        return productionDate;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public void setProductionDate(Instant productionDate) {
        this.productionDate = productionDate;
    }

    public boolean isWithFile() {
        return getFileName() != null && !getFileName().isBlank();
    }
}
