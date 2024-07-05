package com.provoly.virt.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.clients.DatasetVersionService;
import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.search.AggregateOperation;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.ItemAggregationDto;
import com.provoly.test.*;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.item.DataSourceItemsService;
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
public class AggregateServiceDatasetClosedTest {

    @Inject
    AuthService authService;
    @Inject
    TestDataService testDataService;
    @RestClient
    @Inject
    DatasetVersionService datasetVersionService;
    @InjectKafkaCompanion
    KafkaCompanion companion;
    @Inject
    ItemsTestTools itemsTestTools;
    @Inject
    DataSourceItemsService dataSourceItemsService;
    @Inject
    @RestClient
    DataSourceServiceMock dsMock;

    public void authenticate() {
        authService.authenticate();
    }

    Map<Storage, StorageDataAggregate> dataStorages = Map.of(
            Storage.ELASTIC, new StorageDataAggregate(),
            Storage.POSTGIS, new StorageDataAggregate());

    public static class StorageDataAggregate extends StorageData {
        public AttributeDefDto stringAttribute, intAttribute, geoAttribute;

    }

    public void prepareData(Storage storage) {
        authService.authenticate();
        var dataStorage = dataStorages.get(storage);

        var stringField = testDataService.createField("StringField_%s".formatted(UUID.randomUUID()), "keyword");
        var intField = testDataService.createField("IntegerField_%s".formatted(UUID.randomUUID()), "integer");
        var geoField = testDataService.createField("GeoField_%s".formatted(UUID.randomUUID()), "Point", "EPSG:4326");

        dataStorage.stringAttribute = testDataService.createAttribute("StringField", stringField);
        dataStorage.intAttribute = testDataService.createAttribute("IntegerField", intField);
        dataStorage.geoAttribute = testDataService.createAttribute("GeoField", geoField);

        var csvClass = testDataService.createClass(companion, "csvImport", storage,
                dataStorage.stringAttribute,
                dataStorage.intAttribute,
                dataStorage.geoAttribute);
        dataStorage.datasetDto = testDataService.createClosedDataset("csvImport", csvClass.getId());
        dsMock.addDataSource(dataStorage.datasetDto.getId(), DataSourceType.DATASET, dataStorage.datasetDto.getoClass());
    }

    private void insertItems(StorageDataAggregate dataStorage, DatasetVersionDto datasetVersionDto) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(dataStorage.stringAttribute.getName(), "123 AA6 789");
        attributes.put(dataStorage.intAttribute.getName(), 3);
        attributes.put(dataStorage.geoAttribute.getName(),
                new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [30.0, 10.0] }")); // Gradignan
        itemsTestTools.addItem(datasetVersionDto, attributes);

        attributes = new HashMap<>();
        attributes.put(dataStorage.stringAttribute.getName(), "AAA");
        attributes.put(dataStorage.intAttribute.getName(), 33);
        attributes.put(dataStorage.geoAttribute.getName(),
                new GeoHolder("{ \"type\": \"Point\", \"coordinates\": [30.0, 10.0] }")); // Ile de la Cité
        itemsTestTools.addItem(datasetVersionDto, attributes);
    }

    @AfterAll
    public void clean() {
        testDataService.clean();
    }

    @ParameterizedTest
    @EnumSource(names = "POSTGIS")
    @Order(1)
    public void should_countOnLastActiveDatasetVersionWhenAggregationWithoutSpecifiedDatasetVersion(Storage storage) {
        prepareData(storage);
        var dataStorage = dataStorages.get(storage);
        createDatasetVersions(dataStorage);
        createDatasetVersions(dataStorage);

        var result = dataSourceItemsService.getAggregationResult(dataStorage.datasetDto.getId(), new AggregationParamDto(),
                null,
                false, 0);

        assertThat(result.operation()).isEqualTo(AggregateOperation.COUNT);
        assertThat(result.values()).extracting(ItemAggregationDto::getKey).containsExactly("result");
        assertThat(result.values()).extracting(ItemAggregationDto::getValue).containsExactly(2L);

    }

    private void createDatasetVersions(StorageDataAggregate dataStorage) {
        var datasetVersionDto = new DatasetVersionDto(UUID.randomUUID(),
                dataStorage.datasetDto.getId(), dataStorage.datasetDto.getoClass(),
                DatasetState.INDEXING, null, "producer", Instant.now());
        datasetVersionService.create(datasetVersionDto);
        insertItems(dataStorage, datasetVersionDto);
        datasetVersionService.activate(datasetVersionDto.getId());
    }
}