package com.provoly.virt.search;

import static com.provoly.virt.test.KuzzleTestService.KUZZLE_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.test.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.search.mono.MonoClassSearchService;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.KuzzleTestService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AggregateServiceTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    MonoClassSearchService searchService;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    KuzzleTestService kuzzleTestService;

    Map<Storage, StorageDataAggregate> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataAggregate(),
            Storage.POSTGIS, new StorageDataAggregate(),
            Storage.KUZZLE_ASSET, new StorageDataAggregate(),
            Storage.KUZZLE_MEASURE, new StorageDataAggregate());

    private boolean initialized = false;

    public static class StorageDataAggregate extends StorageData {
        public AttributeDefDto attributeIdVehicle, attributeChoc, attributePosition, attributeDate, attributePositionEmpty,
                attributeString;

    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        var dataStorage = dataStorages.get(storage);

        var idVehicleField = testData.createField("aggregate_id", Type.KEYWORD);
        var chocField = testData.createField("aggregate_number", Type.INTEGER);
        var positionField = testData.createField("aggregate_position", Type.POINT, "EPSG:4326");
        var positionFieldEmpty = testData.createField("aggregate_positionEmpty", Type.POINT, "EPSG:4326");
        var dateField = testData.createField("aggregate_date", Type.INSTANT);
        var stringField = testData.createField("aggregate_string", Type.STRING);

        dataStorage.attributeIdVehicle = testData.createAttribute("aggregate_id_vehicle", idVehicleField);
        dataStorage.attributeString = testData.createAttribute("aggregate_string", stringField);
        dataStorage.attributeChoc = testData.createAttribute("aggregate_choc", chocField);
        dataStorage.attributePosition = testData.createAttribute("aggregate_position", positionField);
        dataStorage.attributeDate = testData.createAttribute("aggregate_dateCrea", dateField);
        dataStorage.attributePositionEmpty = testData.createAttribute(
                "aggregate_positionEm", "aggregate_positionEmpty", positionFieldEmpty);

        List<MetadataValueWriteDto> metadata = new ArrayList<>(List.of(
                new MetadataValueWriteDto("AssetTest", MetadataSystem.ASSET_MODEL.getId()),
                new MetadataValueWriteDto("measureTest", MetadataSystem.MEASURE_NAME.getId())));

        var vehicleClass = testData.createClassWithId(companion, UUID.randomUUID(), "aggregate_vehicle", storage, metadata,
                dataStorage.attributeIdVehicle,
                dataStorage.attributeChoc,
                dataStorage.attributePosition,
                dataStorage.attributeDate,
                dataStorage.attributePositionEmpty,
                dataStorage.attributeString);
        dataStorage.datasetVersionDto = testData.createDataset("aggregate_vehicle", vehicleClass.getId());

        GeoHolder point = new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [30.0, 10.0] }");
        Map<String, Object> item1 = Map.of(
                dataStorage.attributeIdVehicle.getName(), "123 AA6 789",
                dataStorage.attributeChoc.getName(), 3,
                dataStorage.attributePosition.getName(), point,
                dataStorage.attributeDate.getName(), "2015-01-01T00:00:00Z");

        Map<String, Object> item2 = Map.of(
                dataStorage.attributeIdVehicle.getName(), "123 AA6 789",
                dataStorage.attributeChoc.getName(), 10,
                dataStorage.attributePosition.getName(), point,
                dataStorage.attributeDate.getName(), "2015-01-01T00:00:00Z");

        Map<String, Object> item3 = Map.of(
                dataStorage.attributeIdVehicle.getName(), "AAA",
                dataStorage.attributeChoc.getName(), 33,
                dataStorage.attributePosition.getName(), point,
                dataStorage.attributeDate.getName(), "2016-01-01T00:00:00Z");

        if (storage != Storage.KUZZLE_ASSET && storage != Storage.KUZZLE_MEASURE) {
            itemsTestTools.addItem(dataStorage.datasetVersionDto, item1);
            itemsTestTools.addItem(dataStorage.datasetVersionDto, item2);
            itemsTestTools.addItem(dataStorage.datasetVersionDto, item3);
        } else if (KUZZLE_ENABLED) {
            Map<String, Object> model = Map.of(
                    dataStorage.attributeIdVehicle.getName(), Map.of("type", "keyword"),
                    dataStorage.attributeChoc.getName(), Map.of("type", "integer"),
                    dataStorage.attributePosition.getName(), Map.of("type", "geo_shape"),
                    dataStorage.attributeDate.getName(), Map.of("type", "date"));

            if (!initialized) {
                kuzzleTestService.initKuzzleModels(model);
                initialized = true;
            }

            item1 = new HashMap<>(item1);
            item1.put(dataStorage.attributePosition.getName(), point.getAsMap());

            item2 = new HashMap<>(item2);
            item2.put(dataStorage.attributePosition.getName(), point.getAsMap());

            item3 = new HashMap<>(item3);
            item3.put(dataStorage.attributePosition.getName(), point.getAsMap());

            kuzzleTestService.insertKuzzleItem("mycab", "myasset", item1, storage,
                    dataStorage.datasetVersionDto);
            kuzzleTestService.insertKuzzleItem("mycab2", "myasset2", item2, storage,
                    dataStorage.datasetVersionDto);
            kuzzleTestService.insertKuzzleItem("mycab3", "myasset3", item3, storage,
                    dataStorage.datasetVersionDto);
        }
    }

    @AfterAll
    public void cleaning() {
        kuzzleTestService.clearKuzzle("mycab", "mycab2", "mycab3");
        testData.clean();
    }

    public static Stream<Storage> retrieveStorage() {
        if (KUZZLE_ENABLED) {
            return Stream.of(Storage.ELASTIC, Storage.POSTGIS, Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE);
        }
        return Stream.of(Storage.ELASTIC, Storage.POSTGIS);
    }

    public static Stream<Storage> retrieveStoragePostgis() {
        if (KUZZLE_ENABLED) {
            return Stream.of(Storage.POSTGIS, Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE);
        }
        return Stream.of(Storage.POSTGIS);
    }

    public static Stream<Storage> retrieveStorageElastic() {
        if (KUZZLE_ENABLED) {
            return Stream.of(Storage.ELASTIC, Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE);
        }
        return Stream.of(Storage.ELASTIC);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(1)
    public void aggregate_nonNumericFieldValue_should_throwError(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.MAX,
                dataStorage.attributeIdVehicle.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        // WHEN
        Assertions.assertThatThrownBy(() -> searchService.aggregate(params, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "It's not possible to aggregate on the non-numeric attribute %s which has type keyword"
                                .formatted(dataStorage.attributeIdVehicle.getName()));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(2)
    public void aggregate_interval_nonNumericAggregateBy_should_throwError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), 5);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);
        //WHEN
        Assertions.assertThatThrownBy(() -> searchService.aggregate(params, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "It's not possible to aggregate on the non-numeric attribute %s which has type keyword"
                                .formatted(dataStorage.attributeIdVehicle.getName()));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(3)
    public void aggregate_dateInterval_nonDateAggregateBy_should_throwError(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), DateInterval.HOUR);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        Assertions.assertThatThrownBy(() -> searchService.aggregate(params, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Aggregating on date is unavailable for attribute %s that is not a date."
                        .formatted(dataStorage.attributeIdVehicle.getName()));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(5)
    public void aggregate_only_aggregateByIdVehicle_should_countRecords(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("AAA", "123 AA6 789");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(1L, 2L);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(6)
    public void aggregate_only_aggregateByIdVehicle_should_sortAsc(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);
        request.setSort(new SortDto(dataStorage.attributeIdVehicle.getId(), Direction.asc));

        // WHEN
        var result = searchService.aggregate(params, request);

        // THEN
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactly("123 AA6 789", "AAA");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactly(2L, 1L);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    @Order(7)
    public void aggregate_only_aggregateByIdVehicle_should_sortDesc(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.COUNT,
                null,
                new SortAggregate(Direction.desc, OrderBy.KEY));
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        // WHEN
        var result = searchService.aggregate(params, request);

        // THEN
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactly("AAA", "123 AA6 789");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactly(1L, 2L);
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(7)
    public void aggregate_only_aggregateByIdVehicle_should_sortDesc_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), null, null,
                new SortAggregate(Direction.desc, OrderBy.KEY));
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        // WHEN
        var result = searchService.aggregate(params, request);

        // THEN
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactly("123 AA6 789", "AAA");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactly(2L, 1L);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(8)
    public void aggregate_maxChocAttr_descOrder_should_return_maxOrdered_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.MAX,
                dataStorage.attributeChoc.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.MAX);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("123 AA6 789", "AAA");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(33.0, 10.0);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    // Test fail for KUZZLE_ASSET and KUZZLE_MEASURE because they return long key instead of double
    @Order(10)
    public void aggregate_interval_chocAttr_should_countRecords(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeChoc.getId(), 10);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder(0.0, 10.0, 20.0, 30.0);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(1L, 1L, 0L, 1L);
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(11)
    public void aggregate_interval_chocAttr_should_countRecords_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeChoc.getId(), 10);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder(3.0, 13.0, 23.0, 33.0);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(new BigDecimal(2),
                new BigDecimal(0), new BigDecimal(0), new BigDecimal(1));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(12)
    public void aggregate_aggregateByIdVehicle_groupByIdVehicle_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(),
                dataStorage.attributeIdVehicle.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("AAA", "123 AA6 789");
        assertThat(result.values()).flatExtracting(groupeItem -> ((ItemAggregationDto.GroupedItemDto) groupeItem).getValue())
                .extracting(ItemAggregationDto.SimpleItemDto::getKey, ItemAggregationDto.SimpleItemDto::getValue)
                .containsExactlyInAnyOrder(tuple("AAA", 1L), tuple("123 AA6 789", 2L));

    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    @Order(13)
    public void aggregate_medianChocAttr_descOrder_should_ignoreOrder_return_median_notOrdered(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.MEDIAN,
                dataStorage.attributeChoc.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.MEDIAN);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("123 AA6 789", "AAA");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder("6.5", "33.0");
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(14)
    public void aggregate_medianChocAttr_descOrder_should_ignoreOrder_return_median_notOrdered_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.MEDIAN,
                dataStorage.attributeChoc.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.MEDIAN);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("123 AA6 789", "AAA");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(6.5, 33.0);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(15)
    public void aggregate_aggregateByIdVehicle_maxOperation_groupByIdVehicle_postgis(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeIdVehicle.getId(), AggregateOperation.MAX,
                dataStorage.attributeChoc.getId(),
                dataStorage.attributeIdVehicle.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.MAX);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactlyInAnyOrder("AAA", "123 AA6 789");
        assertThat(result.values()).flatExtracting(groupeItem -> ((ItemAggregationDto.GroupedItemDto) groupeItem).getValue())
                .extracting(ItemAggregationDto.SimpleItemDto::getKey, ItemAggregationDto.SimpleItemDto::getValue)
                .containsExactlyInAnyOrder(tuple("123 AA6 789", 10.0), tuple("AAA", 33.0));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(16)
    public void aggregate_empty_should_countRecords(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto();
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).hasSize(1);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(3L);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(17)
    public void aggregate_metric_should_return_max_AttributeId(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeChoc.getId(), AggregateOperation.MAX);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.MAX);
        assertThat(result.values()).hasSize(1);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(33.0);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(18)
    public void aggregate_extent_non_geo_should_throw_error(Storage storage) {
        // GIVEN
        var dataStorage = dataStorages.get(storage);
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeChoc.getId(), AggregateOperation.EXTENT);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()));

        //WHEN
        Assertions.assertThatThrownBy(() -> searchService.aggregate(params, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Aggregation on the non-geo attribute aggregate_choc which has type integer is not possible."
                                .formatted(dataStorage.attributeChoc.getName()));

    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    @Order(19)
    public void aggregate_metric_should_return_extent(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributePosition.getId(), AggregateOperation.EXTENT);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()));

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.EXTENT);
        assertThat(result.values()).hasSize(1);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(Map.of(
                "bottom_right", Map.of("lon", 29.999999972060323, "lat", 9.999999990686774),
                "top_left", Map.of("lon", 29.999999972060323, "lat", 9.999999990686774)));
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(20)
    public void aggregate_metric_should_return_extent_postgis(Storage storage) { // FIXME shoud have the same behaviour than ES, issue #527
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributePosition.getId(), AggregateOperation.EXTENT);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.operation()).isEqualTo(AggregateOperation.EXTENT);
        assertThat(result.values()).hasSize(1);
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(Map.of(
                "bottom_right", Map.of("lon", 30.0, "lat", 10.0),
                "top_left", Map.of("lon", 30.0, "lat", 10.0)));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(21)
    public void aggregate_metric_should_return_extent_empty(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributePositionEmpty.getId(),
                AggregateOperation.EXTENT);
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        var result = searchService.aggregate(params, request);

        //THEN
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactlyInAnyOrder(Map.of());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(21)
    public void aggregate_should_not_work_on_string_attributes(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        // GIVEN
        AggregationParamDto params = new AggregationParamDto(dataStorage.attributeString.getId());
        MonoClassRequestDto request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                List.of(dataStorage.datasetVersionDto.getId()), 5);

        //WHEN
        Assertions.assertThatThrownBy(() -> searchService.aggregate(params, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("It's not possible to aggregate on string/text attribute.");

    }
}
