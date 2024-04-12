package com.provoly.virt.imports;

import java.time.Instant;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.entity.FileType;
import com.provoly.virt.file.MockFileUpload;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.Assert;
import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportControllerTest {

    @Inject
    TestDataService testDataService;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    AuthService authService;

    @Inject
    ImportsController importsController;

    DatasetDto dataset;

    private final String fileOkPath = "src/test/resources/importOk.csv";

    MockFileUpload fileUpload;

    @BeforeAll
    void init() {
        authService.authenticate();
        //Create Close Dataset
        var stringField = testDataService.createField("StringField", "keyword");
        var intField = testDataService.createField("IntegerField", "integer");
        var stringAttribute = testDataService.createAttribute("StringField", stringField);
        var intAttribute = testDataService.createAttribute("IntegerField", intField);
        OClassWriteDto csvClass = testDataService.createClass(companion, "csvImport", stringAttribute,
                intAttribute);
        dataset = testDataService.createClosedDataset("csvImport", csvClass.getId());

        fileUpload = new MockFileUpload("filename", fileOkPath, new FileType("text", "csv"));
    }

    @Test
    @Order(1)
    void should_throw_when_negative_chunkSize() {
        var error = Assert.assertThrows(BusinessException.class, () -> importsController.importNewDataset(dataset.getId(),
                fileUpload, false, -50, new DatasetVersionInformationDto("test", Instant.now())));
        Assert.assertEquals("Chunk size can't be negative or exceed 10000.", error.getMessage());
    }

    @Test
    @Order(2)
    void should_throw_when_no_file() {
        var error = Assert.assertThrows(BusinessException.class, () -> importsController.importNewDataset(dataset.getId(), null,
                false, 50, new DatasetVersionInformationDto("test", Instant.now())));
        Assert.assertEquals("File is mandatory", error.getMessage());
    }

    @Test
    @Order(3)
    public void should_throw_when_no_datasetVersion_informations() {
        var error = Assert.assertThrows(BusinessException.class, () -> importsController.importNewDataset(dataset.getId(),
                fileUpload, false, 50, new DatasetVersionInformationDto(null, Instant.now())));
        Assert.assertEquals("Producer and Production date are mandatory for closed dataset", error.getMessage());
    }

    @Test
    @Order(4)
    public void should_return_loading_datasetVersion() {
        var result = importsController.importNewDataset(dataset.getId(), fileUpload, false, 50,
                new DatasetVersionInformationDto("test", Instant.now()));
        Assert.assertEquals(DatasetState.LOADING, result.getState());
    }

}
