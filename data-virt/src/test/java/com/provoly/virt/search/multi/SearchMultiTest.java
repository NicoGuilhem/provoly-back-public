package com.provoly.virt.search.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.search.FieldConditionDto;
import com.provoly.common.search.MultiClassRequestDto;
import com.provoly.common.search.MultiSearchType;
import com.provoly.common.search.Operator;
import com.provoly.test.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.ItemsSearchResult;
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
public class SearchMultiTest {

    @Inject
    AuthService authService;

    //FIXME DO NOT USE
    @Inject
    TestDataService refService;

    @Inject
    MultiClassSearchService multiClassSearchService;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    Map<Storage, StorageDataMulti> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataMulti(),
            Storage.POSTGIS, new StorageDataMulti());

    public class StorageDataMulti extends StorageData {
        private FieldDto numAcc, idVehicule, catV, date, position;

        private OClassWriteDto voiture, usager;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        var storageData = dataStorages.get(storage);

        // create fields
        storageData.numAcc = refService.createField("Num_Acc_%s".formatted(UUID.randomUUID()), "keyword");
        storageData.idVehicule = refService.createField("id_vehicule_%s".formatted(UUID.randomUUID()), "keyword");
        storageData.catV = refService.createField("cat_v_%s".formatted(UUID.randomUUID()), "string");
        storageData.date = refService.createField("date_%s".formatted(UUID.randomUUID()), "instant", "MONTH");
        storageData.position = refService.createField("position_%s".formatted(UUID.randomUUID()), "Point", "EPSG:4326");

        // create class voiture
        AttributeDefDto numAccAttr = refService.createAttribute("Num_Acc", storageData.numAcc);
        AttributeDefDto idVehiculeAttr = refService.createAttribute("id_vehicule", storageData.idVehicule);
        AttributeDefDto catVAttr = refService.createAttribute("catv", storageData.catV);
        AttributeDefDto catV2Attr = refService.createAttribute("catv2", storageData.catV);
        AttributeDefDto dateDebut = refService.createAttribute("date_debut", storageData.date);
        storageData.voiture = refService.createClass(companion, "voiture", numAccAttr, idVehiculeAttr, catVAttr, catV2Attr,
                dateDebut);

        // create class usager
        AttributeDefDto numAccAttrUsager = refService.createAttribute("num_acc", storageData.numAcc);
        AttributeDefDto idVehiculeAttrUsager = refService.createAttribute("id_vehicule", storageData.idVehicule);
        AttributeDefDto dateCreation = refService.createAttribute("date_creation", storageData.date);
        AttributeDefDto currentPosition = refService.createAttribute("position_actuelle", storageData.position);
        storageData.usager = refService.createClass(companion, "usager", numAccAttrUsager, idVehiculeAttrUsager, dateCreation,
                currentPosition);

        // create dataset multi-voiture
        var multiVoiture = refService.createDataset("multi-vehicule", storageData.voiture.getId());

        // create dataset multi-usager
        var multiUsager = refService.createDataset("multi-usager", storageData.usager.getId());

        // Add item vehicule
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(idVehiculeAttr.getName(), "ZZZ");
        attributes.put(numAccAttr.getName(), "123");
        attributes.put(catVAttr.getName(), "catv");
        attributes.put(dateDebut.getName(), "2000-01-10T00:00:00+01:00");

        itemsTestTools.addItem(multiVoiture, attributes);

        // Add item usager
        attributes = new HashMap<>();
        attributes.put(idVehiculeAttrUsager.getName(), "BBB");
        attributes.put(numAccAttrUsager.getName(), "123");
        attributes.put(dateCreation.getName(), "2000-12-10T00:00:00+01:00");
        attributes.put(currentPosition.getName(),
                new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [48.854986760569076, 2.3479450485479996] }"));

        itemsTestTools.addItem(multiUsager, attributes);
    }

    public void cleaning() {
        refService.clean();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(1)
    public void or_id_vehicule_should_return_item_usager_voiture(Storage storage) {
        prepareData(storage);
        var storageData = dataStorages.get(storage);
        FieldConditionDto fieldConditionDto = new FieldConditionDto(storageData.idVehicule.getId(), "ZZZ"); // in voiture
        FieldConditionDto secondFieldConditionDto = new FieldConditionDto(storageData.idVehicule.getId(), "BBB"); // in usager
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(fieldConditionDto, secondFieldConditionDto));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(2)
    public void or_id_vehicule_not_exists_should_return_none(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.idVehicule.getId(), "toto")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(3)
    public void or_id_vehicule_catv_should_return_item_usager_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.idVehicule.getId(), "CCC"),
                        new FieldConditionDto(storageData.catV.getId(), "catv"),
                        new FieldConditionDto(storageData.idVehicule.getId(), "BBB")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(4)
    public void or_id_vehicule_catv_not_exusts_should_return_item_usager(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.catV.getId(), "toto"),
                        new FieldConditionDto(storageData.idVehicule.getId(), "BBB")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(5)
    public void or_num_acc_both_catv_should_return_item_voiture_usager(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.numAcc.getId(), "123"),
                        new FieldConditionDto(storageData.catV.getId(), "catv")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(6)
    public void or_catv_twice_should_return_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.catV.getId(), "catv"),
                        new FieldConditionDto(storageData.catV.getId(), "catv2")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(7)
    public void and_catv_twice_should_return_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(),
                List.of(new FieldConditionDto(storageData.catV.getId(), "catv"),
                        new FieldConditionDto(storageData.catV.getId(), "catv2")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(8)
    public void and_id_vehicule_num_acc_should_return_none(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.idVehicule.getId(), "BBB"),
                        new FieldConditionDto(storageData.numAcc.getId(), "toto")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(9)
    public void and_id_vehicule_should_return_item_usager(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.idVehicule.getId(), "BBB")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(10)
    public void and_id_vehicule_catv_should_return_item_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.idVehicule.getId(), "ZZZ"),
                        new FieldConditionDto(storageData.catV.getId(), "catv")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(11)
    public void and_num_acc_both_catv_should_return_item_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.numAcc.getId(), "123"),
                        new FieldConditionDto(storageData.catV.getId(), "catv")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(12)
    public void and_only_class_voiture_should_return_item_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(storageData.voiture.getId()),
                List.of(new FieldConditionDto(storageData.numAcc.getId(), "123")));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(13)
    public void and_class_not_exists_should_return_not_found(Storage storage) {
        var storageData = dataStorages.get(storage);
        UUID notExists = UUID.randomUUID();
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(notExists),
                List.of(new FieldConditionDto(storageData.numAcc.getId(), "123")));
        assertThatThrownBy(() -> itemsTestTools.searchMulti(request))
                .isInstanceOf(BusinessException.class)
                .extracting(Throwable::getMessage)
                .isEqualTo("OClass : %s inexistant.".formatted(notExists));

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(14)
    public void and_inside_operator(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.date.getId(), "2000-01-01", "2001-01-01", null, Operator.INSIDE)));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(15)
    public void and_inside_operator_should_not_return(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(),
                List.of(new FieldConditionDto(storageData.date.getId(), "2000-01-01", "2000-01-31", null, Operator.INSIDE)));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(16)
    public void and_position_only_class_usager_should_return_item_usager(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(storageData.usager.getId()),
                List.of(new FieldConditionDto(storageData.position.getId(), "1000", null,
                        "POINT (48.854986760569076 2.3479450485479996)",
                        Operator.DISTANCE)));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(17)
    public void and_position_only_usager_date_in_both_should_return_item_voiture(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.AND, List.of(storageData.usager.getId()),
                List.of(
                        new FieldConditionDto(storageData.position.getId(), "1000", null,
                                "POINT (48.854986760569076 2.3479450485479996)",
                                Operator.DISTANCE),
                        new FieldConditionDto(storageData.date.getId(), "2000-01-01", "2001-01-01", null, Operator.INSIDE)));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(18)
    public void or_position_only_usager_date_in_both_should_return_item_voiture_usager(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(), List.of(
                new FieldConditionDto(storageData.position.getId(), "1000", null, "48.854986760569076, 2.3479450485479996",
                        Operator.DISTANCE),
                new FieldConditionDto(storageData.date.getId(), "2000-01-01", "2001-01-01", null, Operator.INSIDE)));
        ItemsSearchResultDto result = itemsTestTools.searchMulti(request);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(storageData.voiture.getId())).hasSize(1);
        assertThat(result.items().get(storageData.usager.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(19)
    public void or_id_vehicule_should_return_1_item_limit_1(Storage storage) {
        var storageData = dataStorages.get(storage);
        MultiClassRequestDto request = new MultiClassRequestDto(MultiSearchType.OR, List.of(), List.of(
                new FieldConditionDto(storageData.idVehicule.getId(), "ZZZ"),
                new FieldConditionDto(storageData.idVehicule.getId(), "BBB")), 1);
        ItemsSearchResult result = multiClassSearchService.search(request);
        assertEquals(1, result.size());
        assertThat(result.getCount().get(storageData.voiture.getId()).count()).isEqualTo(1);
        assertThat(result.getCount().get(storageData.usager.getId()).count()).isEqualTo(1);
    }

}
