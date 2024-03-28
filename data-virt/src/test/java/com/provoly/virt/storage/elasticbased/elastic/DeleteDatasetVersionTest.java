package com.provoly.virt.storage.elasticbased.elastic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.clients.DatasetService;
import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.test.*;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.SearchLimitProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestProfile(SearchLimitProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteDatasetVersionTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;
    @Inject
    AuthService authService;
    @Inject
    TestDataService testData;
    @Inject
    ItemsTestTools itemsTestTools;
    @Inject
    @RestClient
    DatasetService datasetService;
    @Inject
    @RestClient
    DataSourceServiceMock dsMock;

    Map<Storage, StorageDataSearch> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataSearch(),
            Storage.POSTGIS, new StorageDataSearch());

    public static class StorageDataSearch extends StorageData {
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
        var datasetVersionDto = testData.createDataset("searchServiceTest".toLowerCase(), vehicleClass.getId());
        dsMock.addDataSource(datasetVersionDto.getId(), DataSourceType.DATASET_VERSION, datasetVersionDto.getoClass());
        dsMock.addDataSource(datasetVersionDto.getDataset(), DataSourceType.DATASET, datasetVersionDto.getoClass());

        dataStorages.get(storage).datasetVersionDto = datasetVersionDto;
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
    public void delete_datasetVersion_shouldSucceed(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        UUID datasetVersionId = dataStorage.datasetVersionDto.getId();
        testData.deleteDatasetVersion(dataStorage.datasetVersionDto.getId());

        var result = datasetService.getAllById(datasetVersionId);

        assertThat(result)
                .extracting(DatasetVersionDto::getId)
                .doesNotContain(datasetVersionId)
                .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(2)
    public void delete_unknownDatasetVersion_shouldThrow(Storage storage) {
        UUID datasetVersionId = UUID.randomUUID();

        assertThatThrownBy(() -> testData.deleteDatasetVersion(datasetVersionId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("DatasetVersion : %s inexistant.".formatted(datasetVersionId));
    }

}
