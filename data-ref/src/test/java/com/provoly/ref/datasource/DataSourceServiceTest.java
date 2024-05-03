package com.provoly.ref.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.model.OClass;
import com.provoly.ref.searchrequest.MonoClassSearchRequest;
import com.provoly.ref.searchrequest.SearchRequest;
import com.provoly.ref.user.NamedQuery;
import com.provoly.ref.user.NamedQueryService;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DataSourceServiceTest {

    static NamedQueryService namedQueryService;

    static DatasetVersionRepository datasetVersionRepository;

    static DatasetService datasetService;

    static DataSourceService dataSourceService;

    @BeforeAll
    static void before() {
        namedQueryService = mock(NamedQueryService.class);
        datasetVersionRepository = mock(DatasetVersionRepository.class);
        datasetService = mock(DatasetService.class);
        dataSourceService = new DataSourceService(namedQueryService, datasetService, datasetVersionRepository);
    }

    @Test
    public void getDatasourceDetail_WithDataSetId_ReturnDTOWithTypeDataset() {
        UUID id = UUID.randomUUID();

        Dataset dataset = new Dataset(id);
        dataset.setoClass(new OClass(UUID.randomUUID()));

        DatasetVersion datasetVersion = new DatasetVersion(id);
        datasetVersion.setDataset(dataset);

        when(datasetVersionRepository.findById(id)).thenReturn(datasetVersion);
        when(namedQueryService.findById(id)).thenReturn(null);

        assertThat(dataSourceService.getDataSourceDetails(id).type()).isEqualTo(DataSourceType.DATASET_VERSION);
    }

    @Test
    public void getDatasourceDetail_WithNamedQuery_ReturnDTOWithTypeNamedQuery() {
        UUID id = UUID.randomUUID();

        SearchRequest request = new MonoClassSearchRequest();
        request.id = id;

        NamedQuery namedQuery = new NamedQuery(id, "name");
        namedQuery.setRequest(request);
        when(datasetVersionRepository.findById(id)).thenReturn(null);
        when(namedQueryService.findById(id)).thenReturn(namedQuery);

        assertThat(dataSourceService.getDataSourceDetails(id).type()).isEqualTo(DataSourceType.SEARCH);
    }

    @Test
    public void getDatasourceDetail_WithUnknownId_RaiseException() {
        UUID id = UUID.randomUUID();

        when(datasetVersionRepository.findById(id)).thenReturn(null);
        when(namedQueryService.findById(id)).thenReturn(null);
        when(datasetService.findById(id)).thenReturn(null);

        Assertions.assertThatThrownBy(() -> dataSourceService.getDataSourceDetails(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    public void allDataSourceExist_WithUnknownId_RaiseException() {
        UUID id = UUID.randomUUID();

        when(datasetVersionRepository.findById(id)).thenReturn(null);
        when(namedQueryService.findById(id)).thenReturn(null);
        when(datasetService.findById(id)).thenReturn(null);

        Assertions.assertThatThrownBy(() -> dataSourceService.allDataSourcesExist(List.of(id)))
                .isInstanceOf(BusinessException.class);
    }
}
