package com.provoly.common.exec;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JobInstanceBuilder {

    private final Set<DataSourceProvidingDto> dataSources = new HashSet<>();
    private final Set<DatasetOutcomeDto> datasetOutcomes = new HashSet<>();
    private final Set<ParameterValueDto> parametersValue = new HashSet<>();

    public JobInstanceBuilder withDataSource(ProvidingMethod method, UUID datasourceId) {
        dataSources.add(new DataSourceProvidingDto(method, datasourceId));
        return this;
    }

    public JobInstanceBuilder withDataOutcome(OutcomeMethod method, UUID datasetId) {
        datasetOutcomes.add(new DatasetOutcomeDto(method, datasetId));
        return this;
    }

    public JobInstanceBuilder withParameterValue(String parameterName, String parameterValue) {
        parametersValue.add(new ParameterValueDto(parameterName, parameterValue));
        return this;
    }

    public JobInstanceDto build(UUID jobModelId) {
        return new JobInstanceDto(UUID.randomUUID(), jobModelId, dataSources, datasetOutcomes, parametersValue);
    }

}
