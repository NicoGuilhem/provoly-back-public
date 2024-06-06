package com.provoly.virt.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.*;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.datasource.Search;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.test.*;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataSourceControllerTest {
    @Inject
    AuthService authService;
    @Inject
    ItemsTestTools itemsTestTools;
    @Inject
    DataSourceController dataSourceController;
    @Inject
    TestDataService testData;
    @Inject
    @RestClient
    DataSourceServiceMock dsMock;
    @InjectKafkaCompanion
    KafkaCompanion companion;

    Map<Storage, StorageDataAttribute> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataAttribute(),
            Storage.POSTGIS, new StorageDataAttribute());

    static class StorageDataAttribute extends StorageData {
        public AttributeDefDto attributeDate, attributeChoc, attributeKeyword, multiAttributeKeyword;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();

        var dateField = testData.createField("aggregate_date", Type.INSTANT);
        var attributeDate = testData.createAttribute("aggregate_dateCrea", dateField);
        var attributeChoc = testData.createAttribute("aggregate_choc", testData.createField("aggregate_number", Type.INTEGER));
        var multiAttributeKeyword = testData.createAttributeMulti("aggregate_multi",
                testData.createField("aggregate_multi", Type.KEYWORD), true);
        var attributeKeyword = testData.createAttribute("aggregate_string",
                testData.createField("aggregate_string", Type.KEYWORD));
        var vehicleClass = testData.createClass(companion, "aggregate_vehicle", storage, attributeDate, attributeChoc,
                attributeKeyword, multiAttributeKeyword);
        dataStorages.get(storage).attributeDate = attributeDate;
        dataStorages.get(storage).attributeChoc = attributeChoc;
        dataStorages.get(storage).attributeKeyword = attributeKeyword;
        dataStorages.get(storage).multiAttributeKeyword = multiAttributeKeyword;
        var datasetVersionDto = testData.createDataset("aggregate_vehicle", vehicleClass.getId());
        dsMock.addDataSource(datasetVersionDto.getId(), DataSourceType.DATASET_VERSION, datasetVersionDto.getoClass());
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, datasetVersionDto.getoClass());

        dataStorages.get(storage).datasetVersionDto = datasetVersionDto;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(attributeDate.getTechnicalName(), Instant.parse("2015-01-01T00:00:00+01:00"));
        attributes.put(attributeKeyword.getTechnicalName(), "marie");
        attributes.put(attributeChoc.getTechnicalName(), "12");
        attributes.put(multiAttributeKeyword.getTechnicalName(), "12");
        itemsTestTools.addItem(datasetVersionDto, attributes);

        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put(attributeDate.getTechnicalName(), Instant.parse("2015-01-01T00:00:00+01:00"));
        attributes2.put(attributeKeyword.getTechnicalName(), "MaRie");
        attributes2.put(attributeChoc.getTechnicalName(), "15");
        attributes2.put(multiAttributeKeyword.getTechnicalName(), "12");
        itemsTestTools.addItem(datasetVersionDto, attributes2);
    }

    @AfterAll
    public void cleaning() {
        testData.clean();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(1)
    public void getItemWithLimitOfOne_returnOneElement(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        var result = dataSourceController.getItems(dataStorage.datasetVersionDto.getId(),
                new SortDto(dataStorage.attributeChoc.getId(), Direction.asc),
                null,
                1,
                false);
        assertThat(result.items()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(2)
    public void getItemWithLimitOf10000_throwError400(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        assertThatThrownBy(() -> dataSourceController.getItems(dataStorage.datasetVersionDto.getId(),
                new SortDto(dataStorage.attributeChoc.getId(), Direction.asc),
                null,
                10000,
                false)).isInstanceOf(BusinessException.class)
                .hasMessage("Limit can't be negative or exceed 1000.");
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(3)
    public void getItemAggregate_UppercaseDateIntervalFormat(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String intervalFormat = "HOUR";
        var params = new DataSourceController.AggregationParameters();
        params.dateInterval = intervalFormat;
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeDate.getId();

        var result = dataSourceController.getItemsAggregate(params, null, false, 0);

        assertThat(result).isExactlyInstanceOf(AggregationResultDto.class).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(4)
    public void getItemAggregate_LowercaseDateIntervalFormat(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String intervalFormat = "hour";
        var params = new DataSourceController.AggregationParameters();
        params.dateInterval = intervalFormat;
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeDate.getId();

        var result = dataSourceController.getItemsAggregate(params, null, false, 0);

        assertThat(result).isExactlyInstanceOf(AggregationResultDto.class).isNotNull();

    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(5)
    public void getItemAggregate_UppercaseAndLowercaseDateIntervalFormat(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String intervalFormat = "Hour";
        var params = new DataSourceController.AggregationParameters();
        params.aggregatedBy = dataStorage.attributeDate.getId();
        params.dateInterval = intervalFormat;
        params.dataSourceId = dataStorage.datasetVersionDto.getId();

        var result = dataSourceController.getItemsAggregate(params, null, false, 0);

        assertThat(result).isExactlyInstanceOf(AggregationResultDto.class).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(names = { "POSTGIS" }) //fixme add ELASTIC when #64 will be merged
    @Order(6)
    public void autocompleteCalledWithA_shouldReturnOneElement(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes
                .add(new Search.AttributeByDatasource(dataStorage.attributeKeyword.getId(),
                        dataStorage.datasetVersionDto.getId()));
        Search autoComplete = new Search(attributes, "mar", 5);

        var result = dataSourceController.searchForAttributeValues(autoComplete);

        assertThat(result).containsExactlyInAnyOrder("marie", "MaRie").hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(7)
    public void autocompleteCalledWithar_shouldReturnAllElement(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(
                new Search.AttributeByDatasource(dataStorage.attributeKeyword.getId(),
                        dataStorage.datasetVersionDto.getDataset()));
        Search autoComplete = new Search(attributes, "ar", 5);

        var result = dataSourceController.searchForAttributeValues(autoComplete);

        assertThat(result).containsExactlyInAnyOrder("marie", "MaRie").hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(8)
    public void autocompleteCalledWithUnknownAttribute_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        UUID attributeId = UUID.randomUUID();
        attributes.add(new Search.AttributeByDatasource(attributeId, dataStorage.datasetVersionDto.getId()));
        Search autoComplete = new Search(attributes, "Z", 5);

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(autoComplete))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AttributeId %s is unknown for class %s".formatted(attributeId,
                        dataStorage.datasetVersionDto.getoClass()));
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(9)
    public void autocompleteCalledWithUnknownDatasource_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        UUID datasetId = UUID.randomUUID();
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(new Search.AttributeByDatasource(dataStorage.attributeKeyword.getId(), datasetId));
        Search autoComplete = new Search(attributes, "Z", 5);

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(autoComplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown dataset %s".formatted(datasetId));
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(10)
    public void autocompleteCalledWithNamedquery_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String namedQueryName = "namedQuery" + UUID.randomUUID();
        var request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                Collections.singleton(dataStorage.datasetVersionDto.getId()));
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(new Search.AttributeByDatasource(dataStorage.attributeKeyword.getId(), namedQuery.getId()));
        Search autoComplete = new Search(attributes, "Z", 5);
        dsMock.addDataSource(namedQuery.getId(), DataSourceType.SEARCH, dataStorage.datasetVersionDto.getoClass());

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(autoComplete))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Autocomplete is not implemented on namedquery yet.");
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(11)
    public void autocompleteCalledWithAttributeInteger_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(
                new Search.AttributeByDatasource(dataStorage.attributeDate.getId(), dataStorage.datasetVersionDto.getId()));
        Search autoComplete = new Search(attributes, "a", 5);

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(autoComplete))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Search only implemented on attribute with type keyword.");
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(12)
    public void autocompleteCalledWithMultiValueAttribute_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(
                new Search.AttributeByDatasource(dataStorage.multiAttributeKeyword.getId(),
                        dataStorage.datasetVersionDto.getId()));
        Search autoComplete = new Search(attributes, "a", 5);

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(autoComplete))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Search only implemented on simple value attribute.");
    }

    @Order(13)
    public void autocompleteCalledWithoutAnything_shouldReturnAllElement(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes
                .add(new Search.AttributeByDatasource(dataStorage.attributeKeyword.getId(),
                        dataStorage.datasetVersionDto.getId()));
        Search autoComplete = new Search(attributes, "", 5);

        var result = dataSourceController.searchForAttributeValues(autoComplete);

        assertThat(result).containsExactlyInAnyOrder("marie", "MaRie").hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(14)
    public void initSearchWithBlankValue_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        List<Search.AttributeByDatasource> attributes = new ArrayList<>();
        attributes.add(
                new Search.AttributeByDatasource(dataStorage.multiAttributeKeyword.getId(),
                        dataStorage.datasetVersionDto.getId()));

        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(new Search(attributes, null, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Value search must not be null.");
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(15)
    public void initSearchWithNullAttributes_shouldThrowError(Storage storage) {
        assertThatThrownBy(() -> dataSourceController.searchForAttributeValues(new Search(null, "a", 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Must contains at least one attribute.");
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(16)
    public void getItemAggregate_wrongDateIntervalFormat_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String wrongDateIntervalValue = "TOTO";
        var params = new DataSourceController.AggregationParameters();
        params.dateInterval = wrongDateIntervalValue;
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeDate.getId();

        assertThatThrownBy(() -> dataSourceController.getItemsAggregate(params, null, false, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "No enum constant com.provoly.common.search.DateInterval.%s"
                                .formatted(wrongDateIntervalValue));
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(17)
    public void getItemAggregate_wrongAttributeId_shouldThrowError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var params = new DataSourceController.AggregationParameters();
        params.dateInterval = "Second";
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeChoc.getId();

        assertThatThrownBy(() -> dataSourceController.getItemsAggregate(params, null, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Aggregating on date is unavailable for attribute %s that is not a date."
                        .formatted(dataStorage.attributeChoc.getName()));
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(18)
    public void getItemsSearch_withAndComposedCondition_ShouldReturnItems(Storage storage) {
        var dataStorage = dataStorages.get(storage);

        var attributeCondition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "12", Operator.EQUALS, null);
        var attributeCondition2 = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "mar", Operator.I_CONTAINS,
                null);
        var composedCondition = new AndConditionDto();
        composedCondition.composed.add(attributeCondition1);
        composedCondition.composed.add(attributeCondition2);

        SearchRequestDto searchRequestDto = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(), List.of(),
                composedCondition);

        var result = dataSourceController.getItemsSearch(dataStorage.datasetVersionDto.getId(), null, null, searchRequestDto);

        assertThat(result.items()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(19)
    public void getItemsSearch_withConditionAndFilter_ShouldReturnItems(Storage storage) {
        var dataStorage = dataStorages.get(storage);

        var condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "12", Operator.EQUALS, null);
        FilterDto filterDto = new FilterDto(dataStorage.attributeKeyword.getId(), Operator.I_CONTAINS, "mar");
        SearchRequestDto searchRequestDto = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(), List.of(),
                condition);

        var result = dataSourceController.getItemsSearch(dataStorage.datasetVersionDto.getId(), null, List.of(filterDto),
                searchRequestDto);

        assertThat(result.items()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(20)
    public void should_get_empty_result_when_dataset_doesnt_exist(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var dateField = testData.createField("aggregate_date", Type.INSTANT);
        var attributeDate = testData.createAttribute("aggregate_dateCrea", dateField);
        var attributeChoc = testData.createAttribute("aggregate_choc", testData.createField("aggregate_number", Type.INTEGER));
        var vehicleClass = testData.createClass(companion, "aggregate_vehicle", attributeDate, attributeChoc);
        dataStorage.datasetVersionDto = testData.createDataset("aggregate_vehicle", vehicleClass.getId());
        dsMock.addDataSource(dataStorage.datasetVersionDto.getId(), DataSourceType.DATASET,
                dataStorage.datasetVersionDto.getoClass());

        var result = dataSourceController.getItems(dataStorage.datasetVersionDto.getId(), null, null, 0, false);

        assertThat(result.items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(21)
    public void aggregate_groupBy_withOrder_should_throwError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        var params = new DataSourceController.AggregationParameters();
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeChoc.getId();
        params.valueField = dataStorage.attributeChoc.getId();
        params.groupBy = dataStorage.attributeChoc.getId();
        params.sortAggregate = new SortAggregate(Direction.asc, null);

        //WHEN
        assertThatThrownBy(() -> dataSourceController.getItemsAggregate(params, null, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Sort is not available when a groupBy has been set");
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(22)
    public void aggregate_withLimitNegative_should_throwError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        String intervalFormat = "hour";
        var params = new DataSourceController.AggregationParameters();
        params.dateInterval = intervalFormat;
        params.dataSourceId = dataStorage.datasetVersionDto.getId();
        params.aggregatedBy = dataStorage.attributeDate.getId();

        assertThatThrownBy(() -> dataSourceController.getItemsAggregate(params, null, false, -5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limit can't be negative or exceed");
    }

}
