package com.provoly.virt.search.mono;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
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
public class SearchMonoFullTest {

    private static final long BIG_LONG_VALUE = 0x7fffffffffffffffL; // Value greater an integer can support
    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    Map<Storage, StorageDataAttributes> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataAttributes(),
            Storage.POSTGIS, new StorageDataAttributes());

    public class StorageDataAttributes extends StorageData {
        public AttributeDefDto attributeKeyword, attributeString, attributeInteger, attributeLong;
        public ItemDto vehiculeOne, vehiculeTwo;
        public OClassWriteDto vehiculeClass;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        prepareModel(storage);
        insertItems(dataStorages.get(storage));
    }

    @AfterAll
    public void cleaning() {
        testData.clean();
    }

    private void prepareModel(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var fieldKeyword = testData.createField("keyword", "keyword");
        var fieldString = testData.createField("string", "string");
        var fieldInteger = testData.createField("integer", "integer");
        var fieldLong = testData.createField("long", "long");

        dataStorage.attributeKeyword = testData.createAttribute("attrKeyword", fieldKeyword);
        dataStorage.attributeString = testData.createAttribute("attrString", fieldString);
        dataStorage.attributeInteger = testData.createAttribute("attrInteger", fieldInteger);
        dataStorage.attributeLong = testData.createAttribute("attrLong", fieldLong);

        dataStorage.vehiculeClass = testData.createClass(companion, "vehicule", storage, dataStorage.attributeKeyword,
                dataStorage.attributeString, dataStorage.attributeInteger,
                dataStorage.attributeLong);
        dataStorage.datasetVersionDto = testData.createDataset("vehicule", dataStorage.vehiculeClass.getId());
    }

    private void insertItems(StorageDataAttributes dataStorage) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(dataStorage.attributeKeyword.name, "123 AA6 789");
        attributes.put(dataStorage.attributeString.name, "Ceci est une chaîne de caractères");
        attributes.put(dataStorage.attributeInteger.name, 42);
        attributes.put(dataStorage.attributeLong.name, BIG_LONG_VALUE);
        dataStorage.vehiculeOne = itemsTestTools.addItem(dataStorage.datasetVersionDto, attributes);

        attributes.put(dataStorage.attributeKeyword.name, "733 BGH 78");
        attributes.put(dataStorage.attributeString.name, "Une autre valeur");
        attributes.put(dataStorage.attributeInteger.name, 54);
        attributes.put(dataStorage.attributeLong.name, 4);
        dataStorage.vehiculeTwo = itemsTestTools.addItem(dataStorage.datasetVersionDto, attributes);
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(1)
    public void search_on_keyword(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("123 AA6 789");
        var result = itemsTestTools.searchFull(dataStorage.vehiculeClass.getId(), dataStorage.datasetVersionDto, condition);
        assertThat(result.items().get(dataStorage.vehiculeClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehiculeOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(2)
    public void search_on_string(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("chaîne");
        var result = itemsTestTools.searchFull(dataStorage.vehiculeClass.getId(), dataStorage.datasetVersionDto, condition);
        assertThat(result.items().get(dataStorage.vehiculeClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehiculeOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(3)
    public void search_on_string_no_case(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("une");
        var result = itemsTestTools.searchFull(dataStorage.vehiculeClass.getId(), dataStorage.datasetVersionDto, condition);
        assertThat(result.items().get(dataStorage.vehiculeClass.getId())).extracting("id").containsExactlyInAnyOrder(
                dataStorage.vehiculeOne.getId(),
                dataStorage.vehiculeTwo.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(4)
    public void search_on_int(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto("42");
        var result = itemsTestTools.searchFull(dataStorage.vehiculeClass.getId(), dataStorage.datasetVersionDto, condition);
        assertThat(result.items().get(dataStorage.vehiculeClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehiculeOne.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(5)
    public void search_on_long(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        FullSearchConditionDto condition = new FullSearchConditionDto(Long.toString(BIG_LONG_VALUE));
        var result = itemsTestTools.searchFull(dataStorage.vehiculeClass.getId(), dataStorage.datasetVersionDto, condition);
        assertThat(result.items().get(dataStorage.vehiculeClass.getId())).extracting("id")
                .containsExactly(dataStorage.vehiculeOne.getId());
    }

}
