package com.provoly.exec.model;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.common.exec.ExecutionStatus;

@Entity
public class JobExecution extends EntityId {

    @ManyToOne
    JobInstance instance;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    private Instant executionDate;

    protected JobExecution() {
    } // For JPA

    @Default
    public JobExecution(UUID id, JobInstance instance, ExecutionStatus status, Instant executionDate) {
        super(id);
        this.instance = instance;
        this.status = status;
        this.executionDate = executionDate;
    }

    public String getImage() {
        return instance.getModel().getImage();
    }

    public Collection<DataSourceProviding> getInDataSources() {
        return instance.getInDataSources();
    }

    public Collection<DatasetOutcome> getOutDatasets() {
        return instance.getOutDatasets();
    }

    public boolean haveFileParameter() {
        return instance.haveFileParameter();
    }

    public Collection<ParameterValue> getParametersValue() {
        return instance.getParametersValue();
    }

    public String getFilename(ParameterValue pv) {
        return instance.getParameter(pv).getFilename(); // TODO : Should test parameter is effectively of type file
    }

    @Override
    public String toString() {
        return "JobExecution{" +
                "id=" + id +
                ", instance=" + instance +
                ", status=" + status +
                ", executionDate" + executionDate +
                "} " + super.toString();
    }

    public JobInstance getInstance() {
        return instance;
    }

    public void setInstance(JobInstance instance) {
        this.instance = instance;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Instant getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(Instant executionDate) {
        this.executionDate = executionDate;
    }

}