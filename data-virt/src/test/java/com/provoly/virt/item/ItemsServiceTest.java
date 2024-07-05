package com.provoly.virt.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.item.ItemDto;
import com.provoly.test.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.restassured.RestAssured;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ItemsServiceTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testDataService;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    ItemsController itemsController;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    public void init(Storage storage) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        authService.authenticate();

        var numAcc = testDataService.createField("Num_Acc_%s".formatted(storage), "keyword");
        var geo = testDataService.createField("geo_%s".formatted(storage), "LineString", "EPSG:4326");
        var attribute = testDataService.createAttribute("Num_Acc", numAcc);
        var attributeGeo = testDataService.createAttribute("geo", geo);
        var vehiculeClass = testDataService.createClass(companion, "vehicule", storage, attribute, attributeGeo);
        dataStorages.get(storage).datasetVersionDto = testDataService.createDataset("vehicule", vehiculeClass.getId());
    }

    Map<Storage, StorageData> dataStorages = Map.of(
            Storage.ELASTIC, new StorageData(),
            Storage.POSTGIS, new StorageData());

    @AfterAll
    public void cleaning() {
        testDataService.clean();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(1)
    public void addAnItem_whenDatasetNotExists_return404(Storage storage) {
        init(storage);
        UUID unknownDatasetId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        var datasetVersionDto = new DatasetVersionDto(unknownDatasetId);
        var item = new ItemDto(datasetVersionDto, itemId.toString());
        item.put("Num_Acc", "200000000001L");
        List<ItemDto> itemDtos = List.of(item);

        assertThatThrownBy(() -> itemsController.insert(itemDtos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(unknownDatasetId.toString());
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(2)
    public void addAnItem_whenWithInvalidGeoType_return400(Storage storage) {
        UUID itemId = UUID.randomUUID();

        var item = new ItemDto(dataStorages.get(storage).datasetVersionDto, itemId.toString());
        item.put("geo", new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [30.0, 10.0] }"));

        List<ItemDto> itemDtos = List.of(item);
        assertThatThrownBy(() -> itemsController.insert(itemDtos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FORMAT");
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(3)
    public void addAnItem_whenWithValidGeoType_returnOk(Storage storage) {
        UUID itemId = UUID.randomUUID();

        var item = new ItemDto(dataStorages.get(storage).datasetVersionDto, itemId.toString());

        item.put("geo", new GeoHolder("{ \"type\": \"LineString\", \"coordinates\": [[30.0, 10.0],[31.0, 12.0]] }"));

        itemsController.insert(List.of(item));
        itemsTestTools.checkItemExists(item.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(4)
    public void addAnItem_returnOk(Storage storage) {
        String itemId = UUID.randomUUID().toString();

        var item = new ItemDto(dataStorages.get(storage).datasetVersionDto, itemId);
        item.put("Num_Acc", "200000000001L");

        itemsController.insert(List.of(item));
        itemsTestTools.checkItemExists(item.getId());
    }

    @ParameterizedTest
    @EnumSource(names = "ELASTIC")
    @Order(5)
    public void getAnItem_returnOk(Storage storage) {
        var itemId = itemsTestTools.addItem(dataStorages.get(storage).datasetVersionDto);
        ItemId id = new ItemId(itemId);

        var res = itemsController.get(id);
        assertThat(res).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(6)
    public void addAnItem_withDifferentOClassId(Storage storage) {
        String itemId = dataStorages.get(storage).datasetVersionDto.getId().toString() + "@" + UUID.randomUUID();
        var item = new ItemDto(UUID.randomUUID(), itemId);
        List<ItemDto> itemDtos = List.of(item);

        assertThatThrownBy(() -> itemsController.insert(itemDtos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "All items must have same OclassId thant Dataset OclassId : %s"
                                .formatted(dataStorages.get(storage).datasetVersionDto.getoClass()));
    }
}