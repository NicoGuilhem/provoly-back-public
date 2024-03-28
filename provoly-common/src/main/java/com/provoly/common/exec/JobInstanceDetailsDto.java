package com.provoly.common.exec;

import java.util.Set;
import java.util.UUID;

public class JobInstanceDetailsDto {

    private final UUID id;
    private final JobModelDto model;
    private final Set<DataSourceProvidingDto> inDataSources;
    private final Set<DatasetOutcomeDto> outDatasets;
    private final Set<ParameterValueDto> parametersValue;
    private final Boolean active;

    public JobInstanceDetailsDto(UUID id,
            JobModelDto model,
            Set<DataSourceProvidingDto> inDataSources,
            Set<DatasetOutcomeDto> outDatasets,
            Set<ParameterValueDto> parametersValue,
            Boolean active) {
        this.id = id;
        this.model = model;
        this.inDataSources = inDataSources;
        this.outDatasets = outDatasets;
        this.parametersValue = parametersValue;
        this.active = active;
    }

    public <T extends ParameterDto> T getParameter(ParameterValueDto value) {
        return (T) model.getParameters().stream()
                .filter(p -> p.getName().equals(value.getName()))
                .findAny()
                .get();
    }

    public UUID getId() {
        return id;
    }

    public JobModelDto getModel() {
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

    public Boolean isActive() {
        return active;
    }
}
