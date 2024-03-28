package com.provoly.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DataSourceService;
import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.datasource.DataSourceType;

import io.quarkus.test.Mock;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Mock
@ApplicationScoped
@RestClient
public class DataSourceServiceMock implements DataSourceService {

    private final Map<UUID, DataSourceDetailsDto> dataSources = new HashMap<>();

    public DataSourceServiceMock() {
        addDataSource(DatasetFactory.BIKE_STATION_DATASOURCE_ID, DataSourceType.DATASET_VERSION,
                DatasetFactory.BIKE_STATION_OCLASS_ID);
        addDataSource(DatasetFactory.BIKE_STATION_DATASET, DataSourceType.DATASET,
                DatasetFactory.BIKE_STATION_OCLASS_ID);
    }

    @Override
    public DataSourceDetailsDto getDataSourceDetails(UUID dataSourceId) {
        var dataSourceDetails = dataSources.get(dataSourceId);
        if (dataSourceDetails == null) {
            throw new IllegalStateException("Unknown dataset " + dataSourceId);
        }
        return dataSourceDetails;
    }

    public void addDataSource(UUID id, DataSourceType type, UUID oClassId) {
        dataSources.put(id, new DataSourceDetailsDto(id, type, oClassId));

    }
}
