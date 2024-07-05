package com.provoly.virt.search.mono;

import static com.provoly.virt.test.Assertions.assertThat;
import static com.provoly.virt.test.KuzzleTestService.KUZZLE_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wildfly.common.Assert.assertFalse;

import java.util.*;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.item.AttributeSimpleValueDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.search.*;
import com.provoly.test.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.KuzzleTestService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchMonoTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    KuzzleTestService kuzzleTestService;

    Map<Storage, StorageDataAttributes> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataAttributes(),
            Storage.POSTGIS, new StorageDataAttributes(),
            Storage.KUZZLE_ASSET, new StorageDataAttributes(),
            Storage.KUZZLE_MEASURE, new StorageDataAttributes());
    private boolean initialized = false;

    public class StorageDataAttributes extends StorageData {
        public AttributeDefDto attributeIdVehicule, attributeChoc, attributePosition, attributeDate,
                attributeLambertGeo, attributeString, attributeKeyword;

        public DatasetVersionDto datasetVersionLambert;
        private ItemDto vehicle1, vehicle2, vehicle3, vehiculeLambert;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();

        var dataStorage = dataStorages.get(storage);

        var idVehicleField = testData.createField("id_vehicle_%s".formatted(UUID.randomUUID()), "keyword");
        var chocField = testData.createField("choc_%s".formatted(UUID.randomUUID()), "integer");
        var positionField = testData.createField("position_%s".formatted(UUID.randomUUID()), "Point", "EPSG:4326");
        var dateField = testData.createField("date_%s".formatted(UUID.randomUUID()), "instant", "MONTH");
        var geoShapeLambertField = testData.createField("geo_lambert_%s".formatted(UUID.randomUUID()), "Point", "EPSG:2154");
        var stringField = testData.createField("string_%s".formatted(UUID.randomUUID()), "string");

        dataStorage.attributeIdVehicule = testData.createAttribute("id_vehicle", idVehicleField);
        dataStorage.attributeChoc = testData.createAttribute("choc", chocField);
        dataStorage.attributePosition = testData.createAttribute("position", positionField);
        dataStorage.attributeDate = testData.createAttribute("dateCrea", dateField);
        dataStorage.attributeLambertGeo = testData.createAttribute("geoLambert", geoShapeLambertField);
        dataStorage.attributeString = testData.createAttribute("string", stringField);
        dataStorage.attributeKeyword = testData.createAttribute("keyword", stringField);

        GeoHolder pointItem1 = new GeoHolder(
                "{ \"type\": \"Point\", \"coordinates\": [44.764999072263535, -0.5997965274661601] }");
        Map<String, Object> item1 = Map.of(
                dataStorage.attributeIdVehicule.getName(), "123 AA6 789",
                dataStorage.attributeString.getName(), "123 AA6 789",
                dataStorage.attributeKeyword.getName(), "Marie",
                dataStorage.attributeChoc.getName(), 3,
                dataStorage.attributePosition.getName(), pointItem1,
                dataStorage.attributeDate.getName(), "2015-01-01T00:00:00Z");

        GeoHolder pointItem2 = new GeoHolder(
                "{ \"type\": \"Point\", \"coordinates\": [48.854986760569076, 2.3479450485479996] }");
        Map<String, Object> item2 = Map.of(
                dataStorage.attributeIdVehicule.getName(), "AAA",
                dataStorage.attributeString.getName(), "AAA",
                dataStorage.attributeKeyword.getName(), "marianne",
                dataStorage.attributeChoc.getName(), 33,
                dataStorage.attributePosition.getName(), pointItem2,
                dataStorage.attributeDate.getName(), "2016-01-01T00:00:00Z");

        List<MetadataValueWriteDto> metadata = new ArrayList<>(List.of(
                new MetadataValueWriteDto("AssetTest", MetadataSystem.ASSET_MODEL.getId()),
                new MetadataValueWriteDto("measureTest", MetadataSystem.MEASURE_NAME.getId())));

        var vehicleClass = testData.createClassWithId(companion, UUID.randomUUID(), "vehicle", storage, metadata,
                dataStorage.attributeIdVehicule,
                dataStorage.attributeChoc, dataStorage.attributePosition, dataStorage.attributeDate,
                dataStorage.attributeString, dataStorage.attributeKeyword);

        dataStorage.datasetVersionDto = testData.createDataset("vehicle", vehicleClass.getId());

        if (storage != Storage.KUZZLE_ASSET && storage != Storage.KUZZLE_MEASURE) {
            dataStorage.vehicle1 = itemsTestTools.addItem(dataStorage.datasetVersionDto, item1);
            dataStorage.vehicle2 = itemsTestTools.addItem(dataStorage.datasetVersionDto, item2);
        } else if (KUZZLE_ENABLED) {
            Map<String, Object> model = Map.of(
                    dataStorage.attributeIdVehicule.getName(), Map.of("type", "keyword"),
                    dataStorage.attributeString.getName(), Map.of("type", "text"),
                    dataStorage.attributeChoc.getName(), Map.of("type", "integer"),
                    dataStorage.attributePosition.getName(), Map.of("type", "geo_shape"),
                    dataStorage.attributeDate.getName(), Map.of("type", "date"));

            if (!initialized) {
                kuzzleTestService.initKuzzleModels(model);
                initialized = true;
            }

            item1 = new HashMap<>(item1);
            item1.put(dataStorage.attributePosition.getName(), pointItem1.getAsMap());

            item2 = new HashMap<>(item2);
            item2.put(dataStorage.attributePosition.getName(), pointItem2.getAsMap());

            dataStorage.vehicle1 = kuzzleTestService.insertKuzzleItem("mycab", "myasset", item1, storage,
                    dataStorage.datasetVersionDto);
            dataStorage.vehicle2 = kuzzleTestService.insertKuzzleItem("mycab2", "myasset2", item2, storage,
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

    public static Stream<Storage> retrieveStorageElastic() {
        if (KUZZLE_ENABLED) {
            return Stream.of(Storage.ELASTIC, Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE);
        }
        return Stream.of(Storage.ELASTIC);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(1)
    public void keyword_equals_returnOneResult(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "AAA",
                Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(2)
    public void keyword_equals_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "BB",
                Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(3)
    public void keyword_contains_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "AA",
                Operator.CONTAINS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(4)
    public void keyword_startWith_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "12",
                Operator.START_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(5)
    public void keyword_startWith_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "BB",
                Operator.START_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(6)
    public void keyword_endWith_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "789",
                Operator.END_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(7)
    public void keyword_endWith_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "78",
                Operator.END_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(8)
    public void integer_equals_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "78",
                Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(9)
    public void int_equals_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "3", Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(10)
    public void integer_greaterThan_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "33",
                Operator.GREATER_THAN);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(11)
    public void integer_greaterThan_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2",
                Operator.GREATER_THAN);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(12)
    public void integer_lowerThan_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "34",
                Operator.LOWER_THAN);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(13)
    public void integer_lowerThan_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2",
                Operator.LOWER_THAN);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(14)
    public void or_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        OrConditionDto conditionDtoOr = new OrConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2",
                Operator.LOWER_THAN);
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "Z",
                Operator.CONTAINS);
        conditionDtoOr.composed.add(condition1);
        conditionDtoOr.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoOr);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(15)
    public void or_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        OrConditionDto conditionDtoOr = new OrConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "20",
                Operator.LOWER_THAN);
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "A",
                Operator.CONTAINS);
        conditionDtoOr.composed.add(condition1);
        conditionDtoOr.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoOr);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(16)
    public void or_withWrongAttributeShouldThrowException(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        OrConditionDto conditionDtoOr = new OrConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(UUID.randomUUID(), "20", Operator.LOWER_THAN);
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "A",
                Operator.CONTAINS);
        conditionDtoOr.composed.add(condition1);
        conditionDtoOr.composed.add(condition2);
        assertThatThrownBy(
                () -> itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                        conditionDtoOr))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(17)
    public void and_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "20",
                Operator.LOWER_THAN);
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "3",
                Operator.CONTAINS);

        conditionDtoAnd.composed.add(condition1);
        conditionDtoAnd.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoAnd);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(18)
    public void complexCondition_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        OrConditionDto conditionDtoOr = new OrConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "20",
                Operator.LOWER_THAN);
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "3", Operator.EQUALS);
        conditionDtoOr.composed.add(condition1);
        conditionDtoOr.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoOr);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(19)
    public void complexCondition_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        OrConditionDto conditionDtoOr = new OrConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2",
                Operator.GREATER_THAN);
        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "3",
                Operator.CONTAINS);
        AttributeConditionDto condition3 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "AA",
                Operator.START_WITH);
        conditionDtoAnd.composed.add(condition2);
        conditionDtoAnd.composed.add(new TrueConditionDto());
        conditionDtoAnd.composed.add(condition3);
        conditionDtoOr.composed.add(condition1);
        conditionDtoOr.composed.add(conditionDtoAnd);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoOr);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());

    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(20)
    public void inside_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2", Operator.INSIDE,
                "4");
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "3",
                Operator.CONTAINS);
        conditionDtoAnd.composed.add(condition1);
        conditionDtoAnd.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoAnd);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(21)
    public void outside_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "4", Operator.OUTSIDE,
                "54");
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "3",
                Operator.CONTAINS);
        conditionDtoAnd.composed.add(condition1);
        conditionDtoAnd.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoAnd);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(22)
    public void outside_returnAnyResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "2", Operator.OUTSIDE,
                "34");
        AttributeConditionDto condition2 = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "3",
                Operator.CONTAINS);
        conditionDtoAnd.composed.add(condition1);
        conditionDtoAnd.composed.add(condition2);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                conditionDtoAnd);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(23)
    public void distance_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var condition = new AttributeConditionDto(dataStorage.attributePosition.getId(), "10000", Operator.DISTANCE);
        condition.setLocation("POINT (44.813623245283146 -0.554864455306414)"); // Cité numérique
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertThat(items).haveItemsForClass(dataStorage.datasetVersionDto.getoClass(), dataStorage.vehicle1);
        assertEquals(1, items.count().size());
        assertEquals(1, items.count().get(dataStorage.datasetVersionDto.getoClass()).count());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(24)
    public void asc_returnAllResultSortedByInteger(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributeChoc.getId(), Direction.asc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        assertThat(ids).containsExactly(dataStorage.vehicle1.getId(), dataStorage.vehicle2.getId());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    @Order(25)
    public void asc_returnThrow400WhenSortedByGeoPointOnES(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributePosition.getId(), Direction.asc);

        assertThatThrownBy(
                () -> itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("geopoint");
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(26)
    public void desc_returnAllResultSortedByDate(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributeDate.getId(), Direction.desc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        assertThat(ids).containsExactly(dataStorage.vehicle2.getId(), dataStorage.vehicle1.getId());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(27)
    public void string_iContains_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "MAr",
                Operator.I_CONTAINS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(28)
    public void string_iEquals_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "mariE",
                Operator.I_EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(29)
    public void string_iNotEquals_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "mariAnne",
                Operator.I_NOT_EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(30)
    public void string_iStartWith_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "MaR",
                Operator.I_START_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(31)
    public void string_iEndWith_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeKeyword.getId(), "NnE",
                Operator.I_END_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(32)
    public void intersects_returnAllGeoIntersecting(Storage storage) {
        var dataStorage = dataStorages.get(storage);

        GeoHolder geoHolder = new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [0,0] }");
        Map<String, Object> item3 = Map.of(
                dataStorage.attributeIdVehicule.getName(), "aäa",
                dataStorage.attributePosition.getName(), geoHolder);
        if (storage != Storage.KUZZLE_ASSET && storage != Storage.KUZZLE_MEASURE) {
            dataStorage.vehicle3 = itemsTestTools.addItem(dataStorage.datasetVersionDto, item3);
        } else if (KUZZLE_ENABLED) {
            item3 = new HashMap<>(item3);
            item3.put(dataStorage.attributePosition.getName(), geoHolder.getAsMap());
            dataStorage.vehicle3 = kuzzleTestService.insertKuzzleItem("mycab3", "myasset3", item3, storage,
                    dataStorage.datasetVersionDto);
        }

        String geoValue = "{\"type\": \"Polygon\",\"coordinates\": [[[0.0, 0.0], [0.0, 1.0], [1.0,1.0], [1.0,0.0], [0.0, 0.0]]]}";
        var condition = new AttributeConditionDto(dataStorage.attributePosition.getId(), geoValue, Operator.INTERSECTS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertThat(items).haveItemsForClass(dataStorage.datasetVersionDto.getoClass(), dataStorage.vehicle3);
        assertEquals(1, items.count().size());
        assertEquals(1, items.count().get(dataStorage.datasetVersionDto.getoClass()).count());
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(33)
    public void intersects_returnAllGeoIntersecting_EPSG_2154(Storage storage) {
        var dataStorage = dataStorages.get(storage);

        var vehicleClassLambert = testData.createClass(companion, "vehicleLambert", storage,
                dataStorage.attributeLambertGeo);
        dataStorage.datasetVersionLambert = testData.createDataset("vehicleLambert", vehicleClassLambert.getId());

        Map<String, Object> lambertAttributes = new HashMap<>();
        lambertAttributes.put(dataStorage.attributeLambertGeo.getName(),
                new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [0, 0] }", "EPSG:2154"));
        dataStorage.vehiculeLambert = itemsTestTools.addItem(dataStorage.datasetVersionLambert, lambertAttributes);

        String geoValue = "{\"type\": \"Polygon\", \"coordinates\": [[[0.0, 0.0], [0.0, 1.0], [1.0,1.0], [1.0,0.0], [0.0, 0.0]]]}";
        var condition = new AttributeConditionDto(dataStorage.attributeLambertGeo.getId(), geoValue, Operator.INTERSECTS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionLambert.getoClass(), dataStorage.datasetVersionLambert,
                condition);
        assertThat(items).haveItemsForClass(dataStorage.datasetVersionLambert.getoClass(), dataStorage.vehiculeLambert);
        assertEquals(1, items.count().size());
        assertEquals(1, items.count().get(dataStorage.datasetVersionLambert.getoClass()).count());
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(34)
    public void should_nt_return_geometric_attributes(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionLambert.getoClass(), dataStorage.datasetVersionLambert,
                null, true);
        //Class only has one attribute which is a geometric one
        assertThat(items.items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS") //@MethodSource("retrieveStorage")
    @Order(35)
    public void should_filter_attribute_with_value_3(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var condition = new AttributeConditionDto(dataStorage.attributeChoc.getId(), "3", Operator.NOT_EQUALS);
        var result = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);

        assertThat(result.items().get(dataStorage.datasetVersionDto.getoClass())).size().isEqualTo(1);
        assertThat(result.items().get(dataStorage.datasetVersionDto.getoClass()))
                .flatExtracting(item -> item.getAttributes().values())
                .noneMatch(attribute -> ((AttributeSimpleValueDto) attribute).value.equals(3));
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(36)
    public void asc_returnAllResultSortedByString(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributeIdVehicule.getId());
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        assertThat(ids).containsExactly(dataStorage.vehicle1.getId(), dataStorage.vehicle2.getId(),
                dataStorage.vehicle3.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC") //should be @MethodSource("retrieveStorageElastic") but mapping are different between kuzzle and elastic
    @Order(37)
    public void desc_returnAllResultSortedByString_es_collation(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributeIdVehicule.getId(), Direction.desc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        assertThat(ids).containsExactly(dataStorage.vehicle3.getId(), dataStorage.vehicle2.getId(),
                dataStorage.vehicle1.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(37)
    public void desc_returnAllResultSortedByString_accent_postgis_collation(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(dataStorage.attributeIdVehicule.getId(), Direction.desc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        assertThat(ids).containsExactly(dataStorage.vehicle3.getId(), dataStorage.vehicle2.getId(),
                dataStorage.vehicle1.getId());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorageElastic")
    @Order(38)
    public void desc_returnAllResultSortedById(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(true, Direction.desc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        var orderedById = Stream.of(dataStorage.vehicle3, dataStorage.vehicle2, dataStorage.vehicle1).map(ItemDto::getId)
                .sorted(Comparator.reverseOrder()).toList();
        assertThat(ids).isEqualTo(orderedById);
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(39)
    public void desc_returnAllResultSortedById_postgis(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        SortDto sort = new SortDto(true, Direction.desc);

        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto, sort);
        var ids = items.items().get(dataStorage.datasetVersionDto.getoClass()).stream().map(ItemDto::getId).toList();
        var orderedById = Stream.of(dataStorage.vehicle3, dataStorage.vehicle2, dataStorage.vehicle1).map(ItemDto::getId)
                .sorted(Comparator.reverseOrder()).toList();
        assertThat(ids).isEqualTo(orderedById);
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(40)
    public void trueCondition_returnAllResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);

        var composedConditionDto = new TrueConditionDto();
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                composedConditionDto);
        assertFalse(items.items().isEmpty());
        assertEquals(3, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(41)
    public void text_equals_returnOneResult(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeIdVehicule.getId(), "AAA",
                Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(42)
    public void text_equals_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "BB", Operator.EQUALS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(43)
    public void text_contains_returnTwoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "AA",
                Operator.CONTAINS);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(2, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(44)
    public void text_startWith_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "12",
                Operator.START_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(45)
    public void text_startWith_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "BB",
                Operator.START_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(46)
    public void text_endWith_returnOneResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "789",
                Operator.END_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertFalse(items.items().isEmpty());
        assertEquals(1, items.items().get(dataStorage.datasetVersionDto.getoClass()).size());
    }

    @ParameterizedTest
    @MethodSource("retrieveStorage")
    @Order(47)
    public void text_endWith_returnNoResult(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        AttributeConditionDto condition = new AttributeConditionDto(dataStorage.attributeString.getId(), "78",
                Operator.END_WITH);
        var items = itemsTestTools.searchAll(dataStorage.datasetVersionDto.getoClass(), dataStorage.datasetVersionDto,
                condition);
        assertTrue(items.items().isEmpty());
    }

}
