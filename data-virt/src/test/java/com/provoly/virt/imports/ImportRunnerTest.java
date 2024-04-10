package com.provoly.virt.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import com.provoly.clients.DatasetVersionService;
import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.virt.VirtChangeEvent;
import com.provoly.common.virt.VirtChangeEventImportMessageCreated;
import com.provoly.test.*;
import com.provoly.virt.entity.FileType;
import com.provoly.virt.file.FileService;
import com.provoly.virt.file.MockFileUpload;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportRunnerTest {
    @Inject
    ImportRunner importRunner;

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
    Logger log;

    ConsumerTask<String, VirtChangeEventImportMessageCreated> consumerTask;

    @Inject
    FileService fileService;

    private FileUpload fileUpload;
    private final String fileOkPath = "src/test/resources/importOk.csv";

    private final String fileShapeFileOkPath = "src/test/resources/import.zip";

    @AfterAll
    public void clean() {
        testDataService.clean();
    }

    public void authenticate() {
        authService.authenticate();
    }

    Map<Storage, StorageData> dataStorages = Map.of(Storage.ELASTIC, new StorageData(), Storage.POSTGIS, new StorageData());

    public void prepareKafka() {
        companion.topics().clear();

        companion.registerSerde(VirtChangeEventImportMessageCreated.class,
                new ObjectMapperSerde<>(VirtChangeEventImportMessageCreated.class));

        var consumerBuilder = companion.consume(VirtChangeEventImportMessageCreated.class)
                .withOffsetReset(OffsetResetStrategy.LATEST);
        consumerTask = consumerBuilder.fromTopics(VirtChangeEvent.TOPIC_NAME);
        // Start the consumer
        // We need to wait partition assignment before test started
        // To be sure we don't miss firsts messages
        log.info("Waiting for assignment");
        consumerBuilder.waitForAssignment().await().atMost(Duration.ofSeconds(10));
    }

    public void resetOffset() {
        var consumerBuilder = companion.consume(VirtChangeEventImportMessageCreated.class)
                .withOffsetReset(OffsetResetStrategy.LATEST);
        consumerTask = consumerBuilder.fromTopics(VirtChangeEvent.TOPIC_NAME);
        consumerBuilder.waitForAssignment().await().atMost(Duration.ofSeconds(10));
    }

    public void initDatasetVersion(Storage storageData, String file, MediaType mediaType) throws FileNotFoundException {
        fileUpload = new MockFileUpload("filename", file, mediaType);

        dataStorages.get(storageData).datasetVersionDto = new DatasetVersionDto(UUID.randomUUID(),
                dataStorages.get(storageData).datasetDto.getId(), dataStorages.get(storageData).datasetDto.getoClass(),
                DatasetState.INDEXING, false, "producer", Instant.now());
        datasetVersionService.create(dataStorages.get(storageData).datasetVersionDto);
        fileService.receive(new FileInputStream(fileUpload.filePath().toFile()), mediaType,
                dataStorages.get(storageData).datasetVersionDto.getId());
    }

    public void initGoodDatasetGeo(StorageData storageData) {
        var geoshape = testDataService.createField("geo_shape", "multipolygon", "EPSG:4326");
        var description = testDataService.createField("text", "string");
        var name = testDataService.createField("text", "string");
        var geoshapeAttribute = testDataService.createAttribute("the_geom", geoshape);
        var nameAttribute = testDataService.createAttribute("Name", name);
        // voluntary typo in "description"
        var descriptionAttribute = testDataService.createAttribute("descriptio", description);
        OClassWriteDto geoClass = testDataService.createClass(companion, "geo", geoshapeAttribute, nameAttribute,
                descriptionAttribute);
        storageData.datasetDto = testDataService.createClosedDataset("geo", geoClass.getId());
    }

    @AfterEach
    public void deleteDatasetVersion() {
        dataStorages.values().stream()
                .filter(storage -> storage.datasetVersionDto != null)
                .forEach(storage -> {
                    // Allow to go from INDEXING to ACTIVE then TO INACTIVE, thus allow us to run some mort import Tests
                    datasetVersionService.activate(storage.datasetVersionDto.getId());
                    datasetVersionService.deactivate(storage.datasetVersionDto.getId());
                    storage.datasetVersionDto = null;
                });
    }

    public void initGoodDatasetGeoWithTooMuchAttribute(StorageData storageData) {
        var geoshape = testDataService.createField("geo_shape", "multipolygon", "EPSG:4326");
        var description = testDataService.createField("text", "string");
        var name = testDataService.createField("text", "string");
        var geoshapeAttribute = testDataService.createAttribute("the_geom", geoshape);
        var nameAttribute = testDataService.createAttribute("Name", name);
        // voluntary typo in "description"
        var descriptioAttribute = testDataService.createAttribute("descriptio", description);
        var descriptionAttribute = testDataService.createAttribute("description", description);
        OClassWriteDto geoClass = testDataService.createClass(companion, "geo", geoshapeAttribute, nameAttribute,
                descriptioAttribute, descriptionAttribute);
        storageData.datasetDto = testDataService.createClosedDataset("geo", geoClass.getId());

    }

    public void initFailedDatasetGeo(StorageData storageData) {
        var nom = testDataService.createField("nom_site", "string");
        var ageMin = testDataService.createField("age_minAA", "decimal");
        var ageMax = testDataService.createField("age_max", "decimal");
        var surface = testDataService.createField("surfacess", "string");
        var nomAttribute = testDataService.createAttribute("nom_site", nom);
        var ageMinAttribute = testDataService.createAttribute("age_min", ageMin);
        var ageMaxAttribute = testDataService.createAttribute("age_max", ageMax);
        var surfaceAttribute = testDataService.createAttribute("surfacess", surface);
        OClassWriteDto geoClass = testDataService.createClass(companion, "geo", nomAttribute, ageMaxAttribute,
                ageMinAttribute, surfaceAttribute);
        storageData.datasetDto = testDataService.createClosedDataset("geo", geoClass.getId());
    }

    public void initDatasetCsv(Storage storageData) {
        var stringField = testDataService.createField("StringField", "keyword");
        var intField = testDataService.createField("IntegerField", "integer");
        var dateField = testDataService.createField("InstantField", "instant");
        var geoField = testDataService.createField("GeoField", "MultiLineString", "EPSG:4326");
        var stringAttribute = testDataService.createAttribute("StringField", stringField);
        var intAttribute = testDataService.createAttribute("IntegerField", intField);
        var dateAttribute = testDataService.createAttribute("InstantField", dateField);
        var geoAttribute = testDataService.createAttribute("GeoField", geoField);
        OClassWriteDto csvClass = testDataService.createClass(companion, "csvImport", storageData, stringAttribute,
                intAttribute,
                dateAttribute, geoAttribute);
        dataStorages.get(storageData).datasetDto = testDataService.createClosedDataset("csvImport", csvClass.getId());
    }

    @Test
    @Order(1)
    public void should_throws404whenNoDataset() {
        UUID datasetVersionId = UUID.randomUUID();
        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));

        assertThatThrownBy(() -> importRunner.importItemsFromFile(datasetVersionId, false, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @Order(2)
    public void should_throws404whenChunkSizeNegative() {
        UUID datasetVersionId = UUID.randomUUID();
        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));

        assertThatThrownBy(() -> importRunner.importItemsFromFile(datasetVersionId, false, -1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @Order(3)
    public void should_throws404whenChunkSizeGreaterThanMaxProperty() {
        UUID datasetVersionId = UUID.randomUUID();
        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));

        assertThatThrownBy(() -> importRunner.importItemsFromFile(datasetVersionId, false, 1000000))
                .isInstanceOf(BusinessException.class);
    }

    @Order(4)
    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    public void should_returnOk(Storage storage) throws FileNotFoundException {

        authenticate();
        prepareKafka();
        initDatasetCsv(storage);
        initDatasetVersion(storage, fileOkPath, new FileType("text", "csv"));

        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));
        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), true, 10)).isTrue();

    }

    @Order(5)
    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    public void should_returnKO_withNoNormalization(Storage storage) throws FileNotFoundException {

        initDatasetVersion(storage, fileOkPath, new FileType("text", "csv"));
        String fileNormalization = "src/test/resources/import_normalization_geo.csv";
        fileUpload = new MockFileUpload("filename", fileNormalization, new FileType("text", "csv"));
        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isTrue();

    }

    @Order(6)
    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    public void should_returnOK_withNormalization(Storage storage) throws FileNotFoundException {

        initDatasetVersion(storage, fileOkPath, new FileType("text", "csv"));
        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));
        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), true, 10)).isTrue();

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(7)
    public void should_succeedWithCsvTooMuchAttribute(Storage storage) throws FileNotFoundException {

        String fileOkPathWithTooMuchAttribute = "src/test/resources/importOkWithTooMuchAttribute.csv";
        initDatasetVersion(storage, fileOkPathWithTooMuchAttribute, new FileType("text", "csv"));
        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isTrue();

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(8)
    public void should_setDataSetOnErrorWhenWithNoAttributeOfTheClassInCsv(Storage storage) throws FileNotFoundException {

        String fileKoPath = "src/test/resources/importKo.csv";
        initDatasetVersion(storage, fileKoPath, new FileType("text", "csv"));

        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isFalse();

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(9)
    public void should_setDataSetOnErrorWhenFailedToCastIntoInteger(Storage storage) throws FileNotFoundException {

        resetOffset();
        String fileKoCastPath = "src/test/resources/importKoCast.csv";
        initDatasetVersion(storage, fileKoCastPath, new FileType("text", "csv"));

        Assertions
                .assertFalse(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10));

        var result = consumerTask.awaitRecords(1);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(VirtChangeEventImportMessageCreated.Type.IMPORT_MESSAGE);
        then(event.getImportsMessage().messages().size()).isEqualTo(1);
        then(event.getImportsMessage().messages().get(0).code()).isEqualTo(ExtractMessageCode.FORMAT);

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(10)
    public void should_setDataSetOnErrorWhenFailedToCastAndThrowMaxErrors(Storage storage) throws FileNotFoundException {
        resetOffset();
        String fileKoCastMaxErrorsPath = "src/test/resources/importKoCastMaxErrors.csv";
        initDatasetVersion(storage, fileKoCastMaxErrorsPath, new FileType("text", "csv"));
        UUID datasetVersionId = dataStorages.get(storage).datasetVersionDto.getId();

        Assertions.assertFalse(importRunner.importItemsFromFile(datasetVersionId, false, 10));

        var result = consumerTask.awaitRecords(2);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(VirtChangeEvent.Type.IMPORT_MESSAGE);

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(11)
    public void when_importCsvWithMoreAttributsThanOClass_should_sendWarnings(Storage storage) throws FileNotFoundException {
        resetOffset();
        String fileWarnWithUnknowHeader = "src/test/resources/importUnknowHeader.csv";
        initDatasetVersion(storage, fileWarnWithUnknowHeader, new FileType("text", "csv"));

        Assertions.assertTrue(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10));

        var result = consumerTask.awaitRecords(1);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(VirtChangeEvent.Type.IMPORT_MESSAGE);
        then(event.getImportsMessage().messages().get(0).messageLevel()).isEqualTo(MessageLevel.WARNING);

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(12)
    public void should_succeedWhenNoAllAttributeOfClassProvidedInCsv(Storage storage) throws FileNotFoundException {
        String fileMissingAttribute = "src/test/resources/importMissingAttribute.csv";
        initDatasetVersion(storage, fileMissingAttribute, new FileType("text", "csv"));

        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isTrue();

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(14)
    public void should_succeedWithShapefileNotEnoughAttributes(Storage storage) throws FileNotFoundException {
        initGoodDatasetGeoWithTooMuchAttribute(dataStorages.get(storage));
        initDatasetVersion(storage, fileShapeFileOkPath, new FileType("application", "shp"));

        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isTrue();

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(15)
    public void should_return200WithGoodShapeFile(Storage storage) throws FileNotFoundException {
        initGoodDatasetGeo(dataStorages.get(storage));
        initDatasetVersion(storage, fileShapeFileOkPath, new FileType("application", "shp"));

        Assertions.assertTrue(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10));

    }

    @ParameterizedTest
    @EnumSource(names = { "ELASTIC", "POSTGIS" })
    @Order(16)
    public void should_setDataSetOnErrorWhenNoAttributeOfTheClassInShapefile(Storage storage) throws FileNotFoundException {
        initFailedDatasetGeo(dataStorages.get(storage));
        initDatasetVersion(storage, fileShapeFileOkPath, new FileType("application", "shp"));

        assertThat(importRunner.importItemsFromFile(dataStorages.get(storage).datasetVersionDto.getId(), false, 10)).isFalse();
    }
}