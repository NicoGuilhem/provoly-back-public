package com.provoly.ref.datasource;

import static com.provoly.common.datasource.DataSourceType.*;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.user.NamedQuery;
import com.provoly.ref.user.NamedQueryService;

@ApplicationScoped
public class DataSourceService {

    private NamedQueryService namedQueryService;

    private DatasetService datasetService;
    private DatasetVersionRepository datasetVersionRepository;

    public DataSourceService(NamedQueryService namedQueryService,
            DatasetService datasetService, DatasetVersionRepository datasetVersionRepository) {
        this.namedQueryService = namedQueryService;
        this.datasetService = datasetService;
        this.datasetVersionRepository = datasetVersionRepository;
    };

    @Transactional
    public DataSourceDetailsDto getDataSourceDetails(UUID dataSourceId) {
        return switch (getDataSourceType(dataSourceId)) {
            case DATASET_VERSION -> mapDatasetVersionToDataSource(datasetVersionRepository.findById(dataSourceId));
            case SEARCH -> mapNamedQueryToDatasource(namedQueryService.findById(dataSourceId));
            case DATASET -> mapDatasetToDataSource(datasetService.getById(dataSourceId));
        };
    }

    public void allDataSourcesExist(List<UUID> dataSourceIds) {
        dataSourceIds.forEach(this::getDataSourceType);
    }

    private DataSourceDetailsDto mapNamedQueryToDatasource(NamedQuery namedQuery) {
        return new DataSourceDetailsDto(namedQuery.getId(), SEARCH, namedQuery.getRequest().id);

    }

    private DataSourceDetailsDto mapDatasetVersionToDataSource(DatasetVersion datasetVersion) {
        return new DataSourceDetailsDto(datasetVersion.getId(), DATASET_VERSION,
                datasetVersion.getDataset().getoClass().getId());
    }

    private DataSourceDetailsDto mapDatasetToDataSource(Dataset dataset) {
        return new DataSourceDetailsDto(dataset.getId(), DATASET, dataset.getoClass().getId());
    }

    public DataSourceType getDataSourceType(UUID dataSourceId) {
        if (datasetVersionRepository.findById(dataSourceId) != null) {
            return DATASET_VERSION;
        } else if (namedQueryService.findById(dataSourceId) != null) {
            return SEARCH;
        } else if (datasetService.findById(dataSourceId) != null) {
            return DATASET;
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND, "DataSource not found: " + dataSourceId);
        }
    }
}
