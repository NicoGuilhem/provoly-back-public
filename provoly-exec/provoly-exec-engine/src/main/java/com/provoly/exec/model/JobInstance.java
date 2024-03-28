package com.provoly.exec.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;

@Entity
public class JobInstance extends EntityId {

    @ManyToOne
    private JobModel model;
    @ElementCollection
    private Collection<DataSourceProviding> inDataSources = new ArrayList<>();

    @ElementCollection
    private Collection<DatasetOutcome> outDatasets = new ArrayList<>();

    @ElementCollection
    private Collection<ParameterValue> parametersValue = new ArrayList<>();
    private boolean active;

    protected JobInstance() {
        super();
    }

    @Default // Used by mapstruct
    public JobInstance(UUID id, JobModel model) {
        super(id);
        this.model = model;
        this.active = true;
    }

    public boolean haveFileParameter() {
        return !parametersValue.isEmpty();
    }

    public Parameter getParameter(ParameterValue parameterValue) {
        return model.getParameters().stream()
                .filter(p -> p.getName().equals(parameterValue.getName()))
                .findAny()
                .orElseThrow();
    }

    @Override
    public String toString() {
        return "JobInstance{" +
                "id=" + id +
                ", model=" + model +
                ", dataSourceProvidings=" + inDataSources +
                "} ";
    }

    public JobModel getModel() {
        return model;
    }

    public Collection<DataSourceProviding> getInDataSources() {
        return inDataSources;
    }

    public Collection<DatasetOutcome> getOutDatasets() {
        return outDatasets;
    }

    public Collection<ParameterValue> getParametersValue() {
        return parametersValue;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
