package com.provoly.ref.datasetversion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.ref.dataset.Dataset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetVersionServiceUTest {

    DatasetVersionService datasetVersionService;
    DatasetVersionRepository datasetVersionRepository;

    @BeforeEach
    public void init() {
        datasetVersionRepository = mock(DatasetVersionRepository.class);
        datasetVersionService = new DatasetVersionService(null, null, null, null, datasetVersionRepository);
    }

    @Test
    void test_create_closed_datasetVersion_without_producer_throw_exception() {
        when(datasetVersionRepository.exists(any())).thenReturn(false);

        var dataset = new Dataset(UUID.randomUUID());
        dataset.setType(DatasetType.CLOSED);
        var datasetVersion  = new DatasetVersion(UUID.randomUUID());
        datasetVersion.setProductionDate(Instant.now());
        datasetVersion.setDataset(dataset);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    datasetVersionService.createDatasetVersion(datasetVersion);
                }, "Producer and Production date are mandatory for closed dataset");
    }

    @Test
    void test_create_closed_datasetVersion_without_productionDate_throw_exception() {
        when(datasetVersionRepository.exists(any())).thenReturn(false);

        var dataset = new Dataset(UUID.randomUUID());
        dataset.setType(DatasetType.CLOSED);
        var datasetVersion  = new DatasetVersion(UUID.randomUUID());
        datasetVersion.setProducer("moi");
        datasetVersion.setDataset(dataset);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    datasetVersionService.createDatasetVersion(datasetVersion);
                }, "Producer and Production date are mandatory for closed dataset");
    }

    @Test
    void test_create_closed_datasetVersion_with_productionDate_in_future_throw_exception() {
        when(datasetVersionRepository.exists(any())).thenReturn(false);

        var dataset = new Dataset(UUID.randomUUID());
        dataset.setType(DatasetType.CLOSED);
        var datasetVersion  = new DatasetVersion(UUID.randomUUID());
        datasetVersion.setProductionDate(Instant.now().plusSeconds(60));
        datasetVersion.setProducer("moi");
        datasetVersion.setDataset(dataset);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    datasetVersionService.createDatasetVersion(datasetVersion);
                }, "Production date cannot be in the future");
    }

    @Test
    void test_create_closed_datasetVersion_with_existing_indexing_dataset_version_throw_exception() {
        when(datasetVersionRepository.exists(any())).thenReturn(false);
        when(datasetVersionRepository.countLoadOrIndexingDatasetVersionByDataset(any())).thenReturn(1L);

        var dataset = new Dataset(UUID.randomUUID());
        dataset.setType(DatasetType.CLOSED);
        
        var datasetVersion  = new DatasetVersion(UUID.randomUUID());
        datasetVersion.setProductionDate(Instant.now());
        datasetVersion.setProducer("moi");
        datasetVersion.setDataset(dataset);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    datasetVersionService.createDatasetVersion(datasetVersion);
                }, "Can't override existing dataset %s or have parallel imports.".formatted(datasetVersion.getId()));
    }

    @Test
    void test_create_closed_datasetVersion_with_required_data_succeed() {
        when(datasetVersionRepository.exists(any())).thenReturn(false);
        when(datasetVersionRepository.countLoadOrIndexingDatasetVersionByDataset(any())).thenReturn(0L);

        var dataset = new Dataset(UUID.randomUUID());
        dataset.setType(DatasetType.CLOSED);

        var datasetVersion  = new DatasetVersion(UUID.randomUUID());
        datasetVersion.setProductionDate(Instant.now());
        datasetVersion.setProducer("moi");
        datasetVersion.setDataset(dataset);

        Assertions.assertDoesNotThrow(() -> datasetVersionService.createDatasetVersion(datasetVersion));
    }

}
