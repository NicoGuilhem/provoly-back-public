package com.provoly.virt.search.multi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.search.FullSearchConditionDto;
import com.provoly.test.*;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchMultiFullTest {

    private static final long BIG_LONG_VALUE = 0x7fffffffffffffffL; // Value greater an integer can support
    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    Map<Storage, StorageDataMulti> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataMulti());

    public class StorageDataMulti extends StorageData {
        private OClassWriteDto vehicleClass;
        private OClassWriteDto usagerClass;
        private ItemDto vehicleOne, usagerOne;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();

        var fieldKeyword = testData.createField("keyword_%s".formatted(UUID.randomUUID()), "keyword");
        var fieldString = testData.createField("string_%s".formatted(UUID.randomUUID()), "string");
        var fieldInteger = testData.createField("integer_%s".formatted(UUID.randomUUID()), "integer");
        var fieldLong = testData.createField("long_%s".formatted(UUID.randomUUID()), "long");

        addVehicles(storage, fieldKeyword, fieldString, fieldInteger, fieldLong);
        addUsagers(storage, fieldKeyword, fieldString, fieldInteger, fieldLong);

    }

    @AfterAll
    public void cleaning() {
        testData.clean();
    }

    private void addVehicles(Storage storage, FieldDto fieldKeyword, FieldDto fieldString, FieldDto fieldInteger,
            FieldDto fieldLong) {
        var storageData = dataStorages.get(storage);
        var attributeKeyword = testData.createAttribute("attrKeyword", fieldKeyword);
        var attributeString = testData.createAttribute("attrString", fieldString);
        var attributeInteger = testData.createAttribute("attrInteger", fieldInteger);
        var attributeLong = testData.createAttribute("attrLong", fieldLong);

        storageData.vehicleClass = testData.createClass(companion, "vehicle", storage, attributeKeyword, attributeString,
                attributeInteger,
                attributeLong);
        var datasetVehicle = testData.createDataset("vehicle", storageData.vehicleClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(attributeKeyword.getName(), "Billy");
        attributes.put(attributeString.getName(), "Ceci est une chaîne de méchante caractères");
        attributes.put(attributeInteger.getName(), 42);
        attributes.put(attributeLong.getName(), BIG_LONG_VALUE);
        storageData.vehicleOne = itemsTestTools.addItem(datasetVehicle, attributes);

        attributes.put(attributeKeyword.getName(), "733 BGH 78");
        attributes.put(attributeString.getName(), "autre valeur");
        attributes.put(attributeInteger.getName(), 54);
        attributes.put(attributeLong.getName(), 4);
    }

    private void addUsagers(Storage storage, FieldDto fieldKeyword, FieldDto fieldString, FieldDto fieldInteger,
            FieldDto fieldLong) {
        var storageData = dataStorages.get(storage);
        var attributeKeyword = testData.createAttribute("attrKeyword", fieldKeyword);
        var attributeString = testData.createAttribute("attrString", fieldString);
        var attributeInteger = testData.createAttribute("attrInteger", fieldInteger);
        var attributeLong = testData.createAttribute("attrLong", fieldLong);

        storageData.usagerClass = testData.createClass(companion, "usager", attributeKeyword, attributeString, attributeInteger,
                attributeLong);
        var datasetUsager = testData.createDataset("usager", storageData.usagerClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(attributeKeyword.getName(), "Billy");
        attributes.put(attributeString.getName(), "Je suis une bad Méchante guy");
        attributes.put(attributeInteger.getName(), 42);
        attributes.put(attributeLong.getName(), BIG_LONG_VALUE);
        storageData.usagerOne = itemsTestTools.addItem(datasetUsager, attributes);

        attributes.put(attributeKeyword.getName(), "Martin");
        attributes.put(attributeString.getName(), "Gentil carrossier");
        attributes.put(attributeInteger.getName(), 58);
        attributes.put(attributeLong.getName(), 5);
        itemsTestTools.addItem(datasetUsager, attributes);
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(1)
    public void search_on_keyword(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("Billy");
        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(dataStorage.vehicleClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehicleOne.getId());
        assertThat(result.items().get(dataStorage.usagerClass.getId())).extracting("id")
                .containsExactly(dataStorage.usagerOne.getId());

    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(2)
    public void search_on_string(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("une");
        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(dataStorage.vehicleClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehicleOne.getId());
        assertThat(result.items().get(dataStorage.usagerClass.getId())).extracting("id")
                .containsExactly(dataStorage.usagerOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(3)
    public void search_on_string_no_case(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("méchante");
        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(dataStorage.vehicleClass.getId())).extracting("id")
                .containsExactlyInAnyOrder(dataStorage.vehicleOne.getId());
        assertThat(result.items().get(dataStorage.usagerClass.getId())).extracting("id")
                .containsExactly(dataStorage.usagerOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(4)
    public void search_on_int(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("42");
        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(dataStorage.vehicleClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehicleOne.getId());
        assertThat(result.items().get(dataStorage.usagerClass.getId())).extracting("id")
                .containsExactly(dataStorage.usagerOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(5)
    public void search_on_long(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto(Long.toString(BIG_LONG_VALUE));
        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(dataStorage.vehicleClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehicleOne.getId());
        assertThat(result.items().get(dataStorage.usagerClass.getId())).extracting("id")
                .containsExactly(dataStorage.usagerOne.getId());
        cleaning();
    }

}
