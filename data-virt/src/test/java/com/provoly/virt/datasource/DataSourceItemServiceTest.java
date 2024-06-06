package com.provoly.virt.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.item.CountDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.*;
import com.provoly.test.*;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.item.DataSourceItemsService;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.SearchLimitProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestProfile(SearchLimitProfile.class)
public class DataSourceItemServiceTest {

    public static AtomicInteger count = new AtomicInteger(0);
    @Inject
    Logger log;

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    DataSourceItemsService datasourceItemsService;

    @Inject
    @RestClient
    DataSourceServiceMock dsMock;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private OClassWriteDto vehicleClass;
    private DatasetVersionDto datasetVersionDto;
    private DatasetDto datasetDto;
    private FieldDto idVehicleField;
    private AttributeDefDto attributeIdVehicle;
    private ItemDto vehicleOne;
    private ItemDto vehicleTwo;

    public void prepareData() {
        log.infof("Preparing data");
        authService.authenticate();
        prepareModel();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        insertItems();
    }

    private void prepareModel() {
        idVehicleField = testData.createField("idVehicle", "keyword");
        attributeIdVehicle = testData.createAttribute("idVehicle", idVehicleField);
        vehicleClass = testData.createClass(companion, "vehicle", attributeIdVehicle);
        datasetVersionDto = testData.createDataset("vehicle", vehicleClass.getId());
        datasetDto = testData.createClosedDataset("closed_ds", vehicleClass.getId());
    }

    private void insertItems() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(attributeIdVehicle.getName(), "123 AA6 789");
        vehicleOne = addItem(attributes);
        attributes = new HashMap<>();
        attributes.put(attributeIdVehicle.getName(), "AAA");
        vehicleTwo = addItem(attributes);
    }

    public void cleaning() {
        testData.clean();
    }

    private ItemDto addItem(Map<String, Object> attributes) {
        count.incrementAndGet();
        return itemsTestTools.addItem(datasetVersionDto, attributes);
    }

    @Test
    @Order(1)
    public void getItems_withNamedQueryShouldReturnItems() {
        prepareData();
        String namedQueryName = "namedQuery" + UUID.randomUUID();

        var request = new MonoClassRequestDto(vehicleClass.getId(), Collections.singleton(datasetVersionDto.getId()));
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        dsMock.addDataSource(namedQuery.getId(), DataSourceType.SEARCH, vehicleClass.getId());
        assertThat(datasourceItemsService.getItems(namedQuery.getId(), null, null, 0, false, null).size()).isEqualTo(2);
    }

    @Test
    @Order(2)
    public void getItems_withDatasourceDefinitionShouldReturnItems() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        assertThat(datasourceItemsService.getItems(datasetVersionDto.getDataset(), null, null, 0, false, null).size())
                .isEqualTo(2);
    }

    @Test
    @Order(3)
    public void getItems_withDatasourceDefinitionSortByAscShouldReturnItems() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());

        var sort = new SortDto(attributeIdVehicle.getId(), Direction.asc);
        var result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), sort, null, 0, false, null);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.getItems().get(0).getId().getAsString()).isEqualTo(vehicleOne.getId());
        assertThat(result.getItems().get(1).getId().getAsString()).isEqualTo(vehicleTwo.getId());
    }

    @Test
    @Order(4)
    public void getItems_withDatasourceDefinitionSortByDescShouldReturnItems() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());

        var sort = new SortDto(attributeIdVehicle.getId(), Direction.desc);
        var result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), sort, null, 0, false, null);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.getItems().get(0).getId().getAsString()).isEqualTo(vehicleTwo.getId());
        assertThat(result.getItems().get(1).getId().getAsString()).isEqualTo(vehicleOne.getId());
    }

    @Test
    @Order(5)
    public void getItems_withDatasourceDefinition_AndFilter_ShouldReturnItemsFiltered() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());

        var result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), null,
                List.of(new FilterDto(attributeIdVehicle.getId(), Operator.EQUALS, "AAA")), 0, false, null);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getItems().getFirst().getId().getAsString()).isEqualTo(vehicleTwo.getId());
    }

    @Test
    @Order(6)
    public void getItems_withNamedQuery_EmptyFilter_ShouldReturnItems() {
        String namedQueryName = "namedQuery" + UUID.randomUUID();
        var request = new MonoClassRequestDto(vehicleClass.getId(), Collections.singleton(datasetVersionDto.getId()),
                new AttributeConditionDto(attributeIdVehicle.getId(), "AAA", Operator.EQUALS));
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        dsMock.addDataSource(namedQuery.getId(), DataSourceType.SEARCH, vehicleClass.getId());
        assertThat(datasourceItemsService.getItems(namedQuery.getId(), null, List.of(), 0, false, null).size()).isEqualTo(1);
    }

    @Test
    @Order(7)
    public void getItems_withDatasourceDefinition_AndFilters_ShouldReturnEmpty() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        var result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), null,
                List.of(new FilterDto(attributeIdVehicle.getId(), Operator.EQUALS, "BBB")), 0, false, null);
        assertThat(result.size()).isZero();
    }

    @Test
    @Order(8)
    public void getItems_withDatasourceDefinition_AndSortWithTypeNull_ShouldNotThrowError() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        var result = datasourceItemsService.getItems(datasetVersionDto.getDataset(),
                new SortDto(attributeIdVehicle.getId(), Direction.asc, null), List.of(), 0, false, null);
        assertThat(result.size()).isEqualTo(count.get());
    }

    @Test
    @Order(9)
    public void getItems_withDatasourceDefinition_AndFiltersNotBelongToClass_ShouldThrowError() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        List<FilterDto> filterDtos = List.of(new FilterDto(UUID.randomUUID(), Operator.EQUALS, "BBB"));

        assertThatThrownBy(
                () -> datasourceItemsService.getItems(datasetVersionDto.getDataset(), null, filterDtos, 0, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("doesn't belong to oClass");
    }

    @Test
    @Order(10)
    public void getItems_withNamedQueryMultiClass_AndFilter_ShouldThrowError() {
        String namedQueryName = "namedQuery" + UUID.randomUUID();
        var request = new MultiClassRequestDto(List.of(vehicleClass.getId()), List.of());
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        dsMock.addDataSource(namedQuery.getId(), DataSourceType.SEARCH, vehicleClass.getId());
        List<FilterDto> filterDtos = List.of(new FilterDto(attributeIdVehicle.getId(), Operator.EQUALS, "AAA"));

        assertThatThrownBy(() -> datasourceItemsService.getItems(namedQuery.getId(),
                null, filterDtos, 0, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Filtering on multi class request is not available");
    }

    @Test
    @Order(12)
    public void getItems_withNamedQueryMultiClass_AndNullFilter_ShouldReturnItem() {
        String namedQueryName = "namedQuery" + UUID.randomUUID();
        var request = new MultiClassRequestDto(MultiSearchType.AND, List.of(vehicleClass.getId()),
                List.of(new FieldConditionDto(idVehicleField.id, "AAA")));
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        dsMock.addDataSource(namedQuery.getId(), DataSourceType.SEARCH, vehicleClass.getId());

        assertThat(datasourceItemsService.getItems(namedQuery.getId(), null, null, 0, false, null).size()).isEqualTo(1);
    }

    @Test
    @Order(13)
    public void getAggregateItems_operationCount_withoutValueField() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        AggregationParamDto params = new AggregationParamDto(attributeIdVehicle.getId(), AggregateOperation.COUNT, null);
        var result = datasourceItemsService.getAggregationResult(datasetVersionDto.getDataset(), params, null, false, 0);
        assertThat(result.values()).hasSize(count.get());
    }

    @Test
    @Order(14)
    public void getAggregateItems_operationOtherThanCount_withoutValueField() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        AggregationParamDto params = new AggregationParamDto(attributeIdVehicle.getId(), AggregateOperation.MAX, null);

        assertThatThrownBy(
                () -> datasourceItemsService.getAggregationResult(datasetVersionDto.getDataset(), params, null, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "valueField is missing for operation %s"
                                .formatted(params.operation()));
    }

    @Test
    @Order(15)
    public void should_get_count_value_equal_to_two_when_get_items() {
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        var sort = new SortDto(MetadataSystem.ID.getId(), Direction.asc, SortType.METADATA);

        ItemsSearchResult result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), sort, null, 0, false, null);

        Map<UUID, CountDto> count = new HashMap<>();
        count.put(datasetVersionDto.getDataset(), new CountDto(2, true));
        assertThat(result.getCount()).isEqualTo(count);
    }

    @Test
    @Order(16)
    public void getAggregateOnDatasetWithoutDatasetVersionShouldReturnAnEmptyList() {
        dsMock.addDataSource(datasetDto.getId(), DataSourceType.DATASET, vehicleClass.getId());
        AggregationParamDto params = new AggregationParamDto(attributeIdVehicle.getId(), AggregateOperation.COUNT, null);
        var result = datasourceItemsService.getAggregationResult(datasetDto.getId(), params, null, false, 0);
        assertThat(result.values()).isEmpty();
    }

    @Test
    @Order(17)
    public void should_get_empty_list_of_items_when_get_items() {
        //FIXME I don't have the thinnest idea of what this is testing...
        testData.clean();
        authService.authenticate();
        prepareModel();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(attributeIdVehicle.getName(), "paginate item 3");

        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, vehicleClass.getId());
        var sort = new SortDto(MetadataSystem.ID.getId(), Direction.asc, SortType.METADATA);

        ItemsSearchResult result = datasourceItemsService.getItems(datasetVersionDto.getDataset(), sort, null, 0, false, null);
        assertThat(result.getItems()).isEmpty();
        testData.clean();
    }

}