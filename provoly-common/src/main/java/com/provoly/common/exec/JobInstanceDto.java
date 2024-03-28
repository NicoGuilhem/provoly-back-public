package com.provoly.common.exec;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class JobInstanceDto {

    private final UUID id;
    private final UUID model;
    private final Set<DataSourceProvidingDto> inDataSources;
    private final Set<DatasetOutcomeDto> outDatasets;
    private final Set<ParameterValueDto> parametersValue;

    @Default // For mapstruct
    @JsonCreator
    public JobInstanceDto(UUID id, UUID modelId, Set<DataSourceProvidingDto> inDataSources,
            Set<DatasetOutcomeDto> datasetOutcomes, Set<ParameterValueDto> parametersValue) {
        this.id = id;
        this.model = modelId;
        this.inDataSources = inDataSources == null ? Collections.emptySet() : Collections.unmodifiableSet(inDataSources);
        this.outDatasets = datasetOutcomes;
        this.parametersValue = parametersValue == null ? Collections.emptySet() : Collections.unmodifiableSet(parametersValue);
    }

    @Override
    public String toString() {
        return "JobInstanceDto{" +
                "id=" + id +
                ", model=" + model +
                ", dataSources=" + inDataSources +
                ", datasetOutcomes=" + outDatasets +
                ", parametersValue=" + parametersValue +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public UUID getModel() {
        return model;
    }

    public Set<DataSourceProvidingDto> getInDataSources() {
        return inDataSources;
    }

    public Set<DatasetOutcomeDto> getOutDatasets() {
        return outDatasets;
    }

    public Set<ParameterValueDto> getParametersValue() {
        return parametersValue;
    }
}
