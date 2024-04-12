package com.provoly.ref.datasetversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.error.BusinessException;
import com.provoly.ref.dataset.Dataset;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DatasetVersionServiceUTest {

    DatasetVersionService datasetVersionService;
    DatasetVersionRepository datasetVersionRepository;
    Logger logger = Logger.getLogger(DatasetVersionService.class);

    private DatasetVersion initClosedDatasetVersion(UUID datasetVersionID, UUID datasetId) {
        var dataset = new Dataset(datasetId);
        dataset.setType(DatasetType.CLOSED);

        var datasetVersion = new DatasetVersion(datasetVersionID);

        datasetVersion.setProductionDate(Instant.now());
        datasetVersion.setProducer("moi");
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersion(1);
        datasetVersion.setState(DatasetState.DELETING);
        return datasetVersion;
    }

    @BeforeEach
    public void init() {
        datasetVersionRepository = mock(DatasetVersionRepository.class);
        datasetVersionService = new DatasetVersionService(null, null, null, logger, datasetVersionRepository);
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

    @Test
    void test_update_without_producer_should_throw() {
        var datasetId = UUID.randomUUID();

        UUID datasetVersionId = UUID.randomUUID();
        var oldDatasetVersion = initClosedDatasetVersion(datasetVersionId, datasetId);
        when(datasetVersionRepository.getById(oldDatasetVersion.getId())).thenReturn(oldDatasetVersion);

        var newDatasetVersion = new DatasetVersionInformationDto(null, Instant.now(), "additionalInformation");

        var error = Assertions.assertThrows(BusinessException.class,
                () -> datasetVersionService.update(oldDatasetVersion, newDatasetVersion));
        assertThat(error.getMessage()).isEqualTo("Producer and Production date are mandatory for closed dataset");
    }

    @Test
    void test_update_without_productionDate_should_throw() {
        var datasetId = UUID.randomUUID();

        UUID datasetVersionId = UUID.randomUUID();
        var oldDatasetVersion = initClosedDatasetVersion(datasetVersionId, datasetId);
        when(datasetVersionRepository.getById(oldDatasetVersion.getId())).thenReturn(oldDatasetVersion);

        var newDatasetVersion = new DatasetVersionInformationDto("producer", null, "additionalInformation");

        var error = Assertions.assertThrows(BusinessException.class,
                () -> datasetVersionService.update(oldDatasetVersion, newDatasetVersion));
        assertThat(error.getMessage()).isEqualTo("Producer and Production date are mandatory for closed dataset");
    }

    @Test
    void test_update_producer_and_productionDate_should_succeed() {
        var datasetId = UUID.randomUUID();

        UUID datasetVersionId = UUID.randomUUID();
        var oldDatasetVersion = initClosedDatasetVersion(datasetVersionId, datasetId);
        when(datasetVersionRepository.getById(oldDatasetVersion.getId())).thenReturn(oldDatasetVersion);

        var newDatasetVersion = new DatasetVersionInformationDto("producer", Instant.now(), "additionalInformation");

        Assertions.assertDoesNotThrow(() -> datasetVersionService.update(oldDatasetVersion, newDatasetVersion));
    }

    @Test
    void changeStateDatasetVersion_to_same_state_should_throw() {
        DatasetVersion datasetVersion = initClosedDatasetVersion(UUID.randomUUID(), UUID.randomUUID());
        datasetVersion.setState(DatasetState.LOADING);

        when(datasetVersionRepository.getById(datasetVersion.getId())).thenReturn(datasetVersion);

        var error = Assertions.assertThrows(BusinessException.class,
                () -> datasetVersionService.changeStateDatasetVersion(datasetVersion.getId(), DatasetState.LOADING));
        assertThat(error.getMessage()).isEqualTo(
                "Dataset %s is already in state: %s".formatted(datasetVersion.getId(), DatasetState.LOADING.toString()));
    }

    static Stream<Map<DatasetState, List<DatasetState>>> invalidTransitionProvider() {
        return Stream.of(
                Map.of(DatasetState.LOADING, List.of(DatasetState.INACTIVE, DatasetState.DELETE_ERROR)),
                Map.of(DatasetState.INDEXING,
                        List.of(DatasetState.LOADING, DatasetState.INACTIVE, DatasetState.DELETING, DatasetState.DELETE_ERROR)),
                Map.of(DatasetState.ACTIVE,
                        List.of(DatasetState.LOADING, DatasetState.INDEXING, DatasetState.ERROR, DatasetState.DELETE_ERROR)),
                Map.of(DatasetState.INACTIVE, List.of(DatasetState.LOADING, DatasetState.INDEXING, DatasetState.DELETE_ERROR)),
                Map.of(DatasetState.ERROR,
                        List.of(DatasetState.LOADING, DatasetState.INDEXING, DatasetState.ACTIVE, DatasetState.INACTIVE)),
                Map.of(DatasetState.DELETE_ERROR,
                        List.of(DatasetState.LOADING, DatasetState.INDEXING, DatasetState.ACTIVE, DatasetState.INACTIVE)),
                Map.of(DatasetState.DELETING,
                        List.of(DatasetState.LOADING, DatasetState.INDEXING, DatasetState.ACTIVE, DatasetState.INACTIVE)));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitionProvider")
    void change_to_invalid_state_shoud_throw_exception(Map<DatasetState, List<DatasetState>> invalidTransitions) {
        invalidTransitions.forEach((key, value) -> {
            DatasetVersion datasetVersion = initClosedDatasetVersion(UUID.randomUUID(), UUID.randomUUID());
            datasetVersion.setState(key);

            when(datasetVersionRepository.getById(datasetVersion.getId())).thenReturn(datasetVersion);

            value.forEach(invalidTransition -> {
                var error = Assertions.assertThrows(BusinessException.class,
                        () -> datasetVersionService.changeStateDatasetVersion(datasetVersion.getId(), invalidTransition));
                assertThat(error.getMessage()).isEqualTo(
                        "Dataset cannot transition from %s to %s".formatted(key, invalidTransition));
            });
        });
    }

    static Stream<Map<DatasetState, List<DatasetState>>> validTransitionProvider() {
        return Stream.of(
                Map.of(DatasetState.LOADING, List.of(DatasetState.INDEXING, DatasetState.ERROR)),
                Map.of(DatasetState.INDEXING, List.of(DatasetState.ACTIVE, DatasetState.ERROR)),
                Map.of(DatasetState.ACTIVE, List.of(DatasetState.INACTIVE, DatasetState.DELETING)),
                Map.of(DatasetState.INACTIVE, List.of(DatasetState.ACTIVE, DatasetState.DELETING)),
                Map.of(DatasetState.ERROR, List.of(DatasetState.DELETING)),
                Map.of(DatasetState.DELETING, List.of(DatasetState.DELETE_ERROR)));
    }

    @ParameterizedTest
    @MethodSource("validTransitionProvider")
    void change_to_valid_state_shoud_not_throw_exception(Map<DatasetState, List<DatasetState>> validTransitions) {
        validTransitions.forEach((key, value) -> {
            DatasetVersion datasetVersion = initClosedDatasetVersion(UUID.randomUUID(), UUID.randomUUID());
            datasetVersion.setState(key);

            when(datasetVersionRepository.getById(datasetVersion.getId())).thenReturn(datasetVersion);

            value.forEach(validTransition -> {
                Assertions.assertDoesNotThrow(
                        () -> datasetVersionService.changeStateDatasetVersion(datasetVersion.getId(), validTransition));
                datasetVersion.setState(key);
            });

        });
    }
}
