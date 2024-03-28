package com.provoly.virt.search;

import static com.provoly.virt.test.SearchResultAssert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.search.AndConditionDto;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.common.search.Operator;
import com.provoly.test.*;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NamedSearchTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    Map<Storage, StorageDataNQ> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataNQ(),
            Storage.POSTGIS, new StorageDataNQ());

    public class StorageDataNQ extends StorageData {
        public AttributeDefDto attributeIdVehicle, attributeChoc;
        private ItemDto vehicleOne;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        prepareModel(storage);
        insertItems(dataStorages.get(storage));
    }

    private void prepareModel(Storage storage) {
        var storageData = dataStorages.get(storage);
        var idVehicleField = testData.createField("id_vehicle", "keyword");
        var chocField = testData.createField("choc", "integer");
        storageData.attributeIdVehicle = testData.createAttribute("id_vehicle", idVehicleField);
        storageData.attributeChoc = testData.createAttribute("choc", chocField);
        var vehicleClass = testData.createClass(companion, "vehicle", storageData.attributeIdVehicle,
                storageData.attributeChoc);
        storageData.datasetVersionDto = testData.createDataset("vehicle", vehicleClass.getId());
    }

    private void insertItems(StorageDataNQ storageData) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(storageData.attributeIdVehicle.name, "123 AA6 789");
        attributes.put(storageData.attributeChoc.name, 3);
        storageData.vehicleOne = itemsTestTools.addItem(storageData.datasetVersionDto, attributes);
        attributes = new HashMap<>();
        attributes.put(storageData.attributeIdVehicle.name, "AAA");
        attributes.put(storageData.attributeChoc.name, 33);
    }

    @AfterAll
    public void cleaning() {
        testData.clean();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    public void search_by_named_query_is_functional(Storage storage) {
        prepareData(storage);
        var storageData = dataStorages.get(storage);
        String namedQueryName = "namedQuery" + UUID.randomUUID();

        AndConditionDto conditionDtoAnd = new AndConditionDto();
        AttributeConditionDto condition1 = new AttributeConditionDto(storageData.attributeIdVehicle.id, "AA",
                Operator.CONTAINS);
        AttributeConditionDto condition2 = new AttributeConditionDto(storageData.attributeChoc.id, "3", Operator.EQUALS);
        conditionDtoAnd.composed.add(condition1);
        conditionDtoAnd.composed.add(condition2);

        var request = new MonoClassRequestDto(storageData.datasetVersionDto.getoClass(),
                Collections.singleton(storageData.datasetVersionDto.getId()),
                conditionDtoAnd);
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        var result = itemsTestTools.searchByNamedQuery(namedQuery.getId());

        assertThat(result).haveItemsForClass(storageData.datasetVersionDto.getoClass(), storageData.vehicleOne);

    }

}