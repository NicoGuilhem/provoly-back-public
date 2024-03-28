package com.provoly.virt.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.*;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.search.Direction;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.common.search.SortDto;
import com.provoly.test.*;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.SearchLimitProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestProfile(SearchLimitProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchServiceTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    SearchService searchService;

    Map<Storage, StorageDataSearch> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataSearch(),
            Storage.POSTGIS, new StorageDataSearch());

    public class StorageDataSearch extends StorageData {
        public AttributeDefDto attributeIdVehicle;
    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        prepareModel(storage);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        insertItems(storage);
    }

    private void prepareModel(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var idVehicleField = testData.createField("id_searchServiceTest", "keyword");
        dataStorage.attributeIdVehicle = testData.createAttribute("id_searchServiceTest", idVehicleField);
        var vehicleClass = testData.createClass(companion, "searchServiceTest".toLowerCase(), storage,
                dataStorage.attributeIdVehicle);
        dataStorage.datasetVersionDto = testData.createDataset("searchServiceTest".toLowerCase(), vehicleClass.getId());
    }

    private void insertItems(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(dataStorage.attributeIdVehicle.name, "123 AA6 789");
        itemsTestTools.addItem(dataStorages.get(storage).datasetVersionDto, attributes);
        attributes = new HashMap<>();
        attributes.put(dataStorage.attributeIdVehicle.name, "AAA");
        itemsTestTools.addItem(dataStorages.get(storage).datasetVersionDto, attributes);
        attributes.put(dataStorage.attributeIdVehicle.name, "BBBB");
        itemsTestTools.addItem(dataStorages.get(storage).datasetVersionDto, attributes);
    }

    @AfterAll
    public void cleaning() {
        testData.clean();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(1)
    public void search_monoClassShouldReturnItems(Storage storage) {
        prepareData(storage);
        var datasetVersion = dataStorages.get(storage).datasetVersionDto;
        var request = new MonoClassRequestDto(datasetVersion.getoClass(), Collections.singleton(datasetVersion.getId()), 0);
        assertThat(searchService.search(request).size()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(2)
    public void search_withNamedQueryShouldReturnItems(Storage storage) {
        var datasetVersion = dataStorages.get(storage).datasetVersionDto;
        String namedQueryName = "namedQuery" + UUID.randomUUID();
        var request = new MonoClassRequestDto(datasetVersion.getoClass(), Collections.singleton(datasetVersion.getId()));
        var namedQuery = testData.createNamedQuery(namedQueryName, request);
        assertThat(searchService.searchByNamedQuery(namedQuery.getId(), null, 0).size()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(3)
    public void searchAll_monoClass_SortOnAttribute_ReturnAllItems(Storage storage) {
        var dataStorage = dataStorages.get(storage);
        var sort = new SortDto(dataStorage.attributeIdVehicle.id, Direction.asc);
        var request = new MonoClassRequestDto(dataStorage.datasetVersionDto.getoClass(),
                Collections.singleton(dataStorage.datasetVersionDto.getId()));
        var result = new ArrayList<>();

        searchService.searchAll(request, sort)
                .subscribe().with(items -> result.addAll(items.getItems()));
        await().untilAsserted(() -> assertThat(result).hasSize(3));
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(4)
    public void searchAll_monoClass_SortOnItemId_ReturnAllItems(Storage storage) {
        var datasetVersion = dataStorages.get(storage).datasetVersionDto;
        var sort = new SortDto(true);
        var request = new MonoClassRequestDto(datasetVersion.getoClass(), Collections.singleton(datasetVersion.getId()));
        var result = new ArrayList<>();

        searchService.searchAll(request, sort)
                .subscribe().with(items -> result.addAll(items.getItems()));
        await().untilAsserted(() -> assertThat(result).hasSize(3));
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(5)
    public void searchAll_monoClass_withoutSort_shouldThrowError(Storage storage) {
        var datasetVersion = dataStorages.get(storage).datasetVersionDto;
        var request = new MonoClassRequestDto(datasetVersion.getoClass(), Collections.singleton(datasetVersion.getId()));
        assertThatThrownBy(() -> searchService.searchAll(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("It's not possible to search all items without sorting");

    }

}
