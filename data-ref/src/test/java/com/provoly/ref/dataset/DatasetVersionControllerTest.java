package com.provoly.ref.dataset;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.dataset.*;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.imports.*;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.common.user.Role;
import com.provoly.ref.datasetversion.*;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.metadata.MetadataDefController;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DatasetVersionControllerTest {
    private final UUID datasetId = UUID.fromString("1af54720-8cb9-43c0-b748-7e1b3ad60b76");
    private final UUID datasetVersionId = UUID.fromString("423b5c01-1816-41d4-b358-200000000005");
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    DatasetVersionService datasetVersionService;
    @Inject
    DatasetService datasetService;
    @Inject
    DatasetVersionMapper datasetVersionMapper;
    @Inject
    DatasetMapper datasetMapper;
    @Inject
    DatasetVersionController datasetVersionController;
    @Inject
    MetadataDefController metadataDefController;
    @Inject
    DatasetVersionMessageService datasetVersionMessageService;
    @Inject
    MetadataService metadataService;
    @Inject
    TestService testService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    UserService userService;
    @Inject
    DatasetVersionRepository datasetVersionRepository;
    private DatasetVersionDto datasetVersionDto;
    private OClass oClass;
    private Dataset dataset;
    private MetadataDefDto metadataDefDto;
    private static final String OCLASS_ID = "bdcdf6f7-5ab4-4b79-b521-7d33675186f4";
    private final Instant fixedDate = Instant.ofEpochSecond(500);

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        initOClass(OCLASS_ID, "oClassTest");
    }

    @AfterEach
    @Transactional
    public void delete() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.clean();
    }

    private void initOClass(String OClassId, String name) {
        OClassWriteDto classDtoTest = new OClassWriteDto(UUID.fromString(OClassId), name, new ArrayList<>(), Storage.ELASTIC);
        oClass = modelMapper.toModel(classDtoTest);
        if (!modelService.exists(oClass)) {
            modelService.saveEntity(oClass);
        }
    }

    private void initDataset(DatasetType datasetType) {
        ProvolyUser provolyUser = userService.getCurrentUser();
        dataset = datasetMapper
                .toModel(new DatasetDto(datasetId, "DatasetTest",
                        oClass.getId(), datasetType));
        dataset.setUser(provolyUser);
        datasetService.saveEntity(dataset);
    }

    private void generateDatasetVersionDto(boolean withFile) {
        datasetVersionDto = new DatasetVersionDto(datasetVersionId, datasetId, oClass.getId(),
                DatasetState.ACTIVE, withFile, fixedDate, "author", "SomeInformations");
    }

    private DatasetVersion generateDatasetVersion(DatasetState state, boolean withFile) {
        var dsv = new DatasetVersion(datasetVersionId);
        dsv.setDataset(dataset);
        dsv.setVersion(1);
        dsv.setState(state);
        dsv.setProducer("producer");
        dsv.setProductionDate(Instant.now());
        dsv.setWithFile(withFile);
        return dsv;
    }

    private void saveDatasetVersion() {
        DatasetVersion datasetVersion = datasetVersionMapper.toModel(datasetVersionDto);
        datasetVersionService.createDatasetVersion(datasetVersion);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_FIELD_READ })
    public void role_user_isUnauthorized() {
        given()
                .pathParam("datasetVersionId", UUID.randomUUID())
                .contentType(ContentType.JSON)
                .when()
                .delete("/dataset-versions/id/{datasetVersionId}")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void deleteUnknownDatasetVersion() {
        UUID datasetVersionId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThatThrownBy(() -> datasetVersionController.delete(datasetVersionId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("DatasetVersion : %s inexistant.".formatted(datasetVersionId));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void updateDatasetVersion_returnOK() {
        initDataset(DatasetType.MODIFIABLE);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setName("dataset-version-name");
        dataset.setoClass(oClass);
        dataset.setSlug(oClass.getSlug());
        dataset.setType(DatasetType.CLOSED);
        dataset.setUser(userService.getCurrentUser());
        datasetService.saveEntity(dataset);
        var datasetVersionDto = new DatasetVersionInformationDto("author", Instant.now());

        given()
                .pathParam("datasetVersionId", datasetVersionId)
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .put("/dataset-versions/id/{datasetVersionId}")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void updateCloseDatasetVersion_without_producer_return_badRequest() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setName("dataset-version-name");
        dataset.setoClass(oClass);
        dataset.setSlug(oClass.getSlug());
        dataset.setType(DatasetType.CLOSED);
        dataset.setUser(userService.getCurrentUser());
        datasetService.saveEntity(dataset);
        var datasetVersionDto = new DatasetVersionInformationDto(null, Instant.now());

        given()
                .pathParam("datasetVersionId", datasetVersionId)
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .put("/dataset-versions/id/{datasetVersionId}")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_WRITE, Role.STR_ITEM_WRITE })
    public void deleteDatasetVersion_returnOk() {
        initDataset(DatasetType.MODIFIABLE);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        given()
                .pathParam("datasetVersionId", datasetVersionDto.getId())
                .contentType(ContentType.JSON)
                .when()
                .delete("/dataset-versions/id/{datasetVersionId}")
                .then()
                .statusCode(200);

        assertThat(datasetVersionRepository.getById(datasetVersionId).getState()).isEqualTo(DatasetState.DELETING);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void createClosedDatasetVersion_returnOk() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .post("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void createOpenDatasetVersionWithoutFileId_returnOk() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .post("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void updateDatasetVersion_returnOk() {
        // GIVEN
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        datasetVersionService.activateDatasetVersion(datasetVersionId);
        var secondDatasetVersion = new DatasetVersionDto(UUID.randomUUID(), datasetId, oClass.getId(),
                DatasetState.ACTIVE, false, "author", Instant.now());

        // WHEN
        datasetVersionController.create(secondDatasetVersion);
        assertThat(datasetVersionController.get(secondDatasetVersion.getId()).getVersion()).isEqualTo(2);

        var maxVersion = datasetVersionRepository.getAllByDatasetId(datasetId).stream()
                .max(Comparator.comparing(DatasetVersion::getVersion))
                .map(DatasetVersion::getVersion)
                .orElse(0);

        // THEN
        assertThat(maxVersion).isEqualTo(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void createOpenDatasetVersionWithoutFileId_isnull() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        datasetVersionController.create(datasetVersionDto);

        assertThat(datasetVersionController.get(datasetVersionDto.getId()).isWithFile()).isFalse();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void createOpenDatasetVersionWithoutFileId_isTrue() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        datasetVersionController.create(datasetVersionDto);

        assertThat(datasetVersionController.get(datasetVersionDto.getId()).isWithFile()).isTrue();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void createOpenDatasetVersionWithFileId_shouldThrowError() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(true);

        assertThatThrownBy(() -> datasetVersionController.create(datasetVersionDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "Dataset version %s is open, it cannot be associated to a source file"
                                .formatted(datasetVersionDto.getId()));
    }

    private void generateMetadataDefWithType(VariableType type, String description) {
        metadataDefDto = new MetadataDefDto();
        metadataDefDto.id = UUID.fromString("32c52b41-b197-4e57-8f6b-a8bf53c9c167");
        metadataDefDto.name = UUID.randomUUID().toString();
        metadataDefDto.type = type;
        metadataDefDto.description = description;
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void addMetadataToDatasetVersion_returnOK() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        datasetVersionController.create(datasetVersionDto);
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);
        var metadataValueWrite = new MetadataValueWriteDto();
        metadataValueWrite.setValue("12");

        datasetVersionController.setMetadata(datasetVersionDto.getId(), metadataDefDto.id, metadataValueWrite);

        assertThat(datasetVersionController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetVersionDetailsDto.class)
                .extracting(DatasetVersionDetailsDto::getMetadata)
                .extracting(x -> x.get(0).getValue())
                .isEqualTo("12");
        deleteMetadataValueOfDatasetVersion();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void deleteMetadataOfDatasetVersion_returnOK() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        datasetVersionController.create(datasetVersionDto);
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);
        var metadataValueWrite = new MetadataValueWriteDto();
        metadataValueWrite.setValue("12");

        datasetVersionController.setMetadata(datasetVersionDto.getId(), metadataDefDto.id, metadataValueWrite);

        datasetVersionController.deleteMetadata(datasetVersionDto.getId(), metadataDefDto.id);

        assertThat(datasetVersionController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetVersionDetailsDto.class)
                .extracting(DatasetVersionDetailsDto::getMetadata)
                .isEqualTo(List.of());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void deleteNonexistentMetadata_shouldThrowError() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        datasetVersionController.create(datasetVersionDto);
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);

        assertThatThrownBy(() -> datasetVersionController.deleteMetadata(datasetVersionDto.getId(), metadataDefDto.id))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Metadata %s is not assigned to dataset_version %s".formatted(metadataDefDto.id,
                                datasetVersionDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void post_closedDatasetCreation_when_haveFile_should_beInitAtStateLoading() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersionDto));

        assertThat(datasetVersionController.get(datasetVersionDto.getId()).getState())
                .isEqualTo(DatasetState.LOADING);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void post_closedDatasetCreation_when_doNotHaveFile_should_beInitAtStateIndexing() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersionDto));

        assertThat(datasetVersionController.get(datasetVersionDto.getId()).getState())
                .isEqualTo(DatasetState.INDEXING);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void post_openDatasetCreation_should_beInitAtStateActivate() {
        initDataset(DatasetType.OPEN);
        generateDatasetVersionDto(false);
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersionDto));

        assertThat(datasetVersionController.get(datasetVersionDto.getId()).getState())
                .isEqualTo(DatasetState.ACTIVE);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void post_modifiableDataset_should_beInitAtStateActive() {
        initDataset(DatasetType.MODIFIABLE);
        generateDatasetVersionDto(false);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .post("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void deactivateDataset_should_setStateAtInactive() {
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.ACTIVE, true);
        datasetVersionRepository.save(dsv);
        datasetVersionService.deactivateDatasetVersion(datasetVersionId);
        assertThat(datasetVersionRepository.getById(datasetVersionId).getState()).isEqualTo(DatasetState.INACTIVE);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void importDataset_when_anotherOneIsImporting_should_ReturnOK() {
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.LOADING, true);
        datasetVersionRepository.save(dsv);

        DatasetDto anotherDatasetDto = new DatasetDto(UUID.randomUUID(), "another one", oClass.getId(), DatasetType.CLOSED);
        datasetService.save(anotherDatasetDto);
        DatasetVersionDto anotherDatasetVersion = new DatasetVersionDto(UUID.randomUUID(), anotherDatasetDto.getId(),
                oClass.getId(), DatasetState.LOADING, "author", Instant.now());

        given()
                .contentType(ContentType.JSON)
                .body(anotherDatasetVersion)
                .when()
                .post("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void importDataset_when_linkedDatasetVersionIsAlreadyImporting_should_ThrowConflict() {
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.LOADING, true);
        datasetVersionRepository.save(dsv);

        DatasetVersionDto anotherDatasetVersionWithSameDataset = new DatasetVersionDto(UUID.randomUUID(), datasetId,
                oClass.getId(), DatasetState.LOADING, "author", Instant.now());

        given()
                .contentType(ContentType.JSON)
                .body(anotherDatasetVersionWithSameDataset)
                .when()
                .post("/dataset-versions")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_READ })
    public void get_DatasetVersionError_should_returnFiveErrorsAndFiveWarnings() {
        // GIVEN
        int errorsCount = 8;
        int warningCount = 6;
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.ERROR, true);
        datasetVersionRepository.save(dsv);

        List<ExtractedMessage> errors = new ArrayList<>();
        for (int i = 0; i < errorsCount; i++) {
            errors.add(new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.FORMAT,
                    new FileImportDto.ParamsTypeError(String.valueOf(i), Type.DECIMAL)));
        }

        List<ExtractedMessage> warnings = new ArrayList<>();
        for (int i = 0; i < warningCount; i++) {
            errors.add(new ExtractedMessage(MessageLevel.WARNING, ExtractMessageCode.UNRECOGNIZED,
                    new FileImportDto.ParamsTypeError(String.valueOf(i), Type.DECIMAL)));
        }

        datasetVersionMessageService.save(new ImportsMessage(dsv.getId(), "1", errors));
        datasetVersionMessageService.save(new ImportsMessage(dsv.getId(), null, warnings));

        // WHEN
        var datasetVersionPreview = datasetVersionController.getDatasetVersionPreviews(dsv.getId());

        // THEN
        assertThat(datasetVersionPreview.get(0).count()).isEqualTo(8);
        assertThat(datasetVersionPreview.get(1).count()).isEqualTo(6);
        assertThat(datasetVersionPreview.get(0).messages()).hasSize(DatasetVersionRepository.PREVIEW_MAX_RESULT);
        assertThat(datasetVersionPreview.get(1).messages()).hasSize(DatasetVersionRepository.PREVIEW_MAX_RESULT);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_READ })
    public void get_DatasetVersionError_should_returnEmpty() {
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.ERROR, true);
        datasetVersionRepository.save(dsv);
        var datasetVersionPreview = datasetVersionController.getDatasetVersionPreviews(dsv.getId());
        assertThat(datasetVersionPreview).isEmpty();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_READ })
    public void get_DatasetVersionError_should_ThrowNotFound() {
        assertThatThrownBy(() -> datasetVersionController.getDatasetVersionPreviews(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void createSameDatasetVersionTwoTimes_shouldThrow() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        datasetVersionController.create(datasetVersionDto);

        assertThatThrownBy(() -> datasetVersionController.create(datasetVersionDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Dataset version %s already exist.".formatted(datasetVersionDto.getId()));
    }

    private void deleteMetadataValueOfDatasetVersion() {
        if (metadataDefDto != null) {
            metadataService.deleteMetadataValueByEntityId(datasetVersionId, metadataDefDto.id,
                    EntityType.DATASET_VERSION);
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void getAllActiveDatasetVersionForClass_shouldSucceed() {
        initDataset(DatasetType.CLOSED);
        var dsv = generateDatasetVersion(DatasetState.ACTIVE, false);
        datasetVersionRepository.save(dsv);

        var result = datasetVersionRepository.getAllActiveForClass(UUID.fromString(OCLASS_ID));

        assertThat(result).hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void getZeroDatasetVersionOfADataset_shouldThrow() {
        initDataset(DatasetType.CLOSED);

        assertThatThrownBy(() -> datasetVersionRepository.getByDatasetId(datasetId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No dataset version found for dataset %s.".formatted(datasetId));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void deleteDatasetVersionIndexing_shouldThrow() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        datasetVersionController.create(datasetVersionDto);

        assertThatThrownBy(() -> datasetVersionController.delete(datasetVersionDto.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unable to delete dataset version %s because it's %s".formatted(datasetVersionDto.getId(),
                        DatasetState.LOADING));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE, Role.STR_DATASET_READ,
            Role.STR_SEARCH })
    public void getDatasetVersionAfterDeletingIt_shouldThrow() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        datasetVersionController.create(datasetVersionDto);

        datasetVersionService.deleteDatasetVersionAfterDeletingItems(datasetVersionDto.getId());

        assertThatThrownBy(() -> datasetVersionController.get(datasetVersionDto.getId()))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessage("%s : %s inexistant.".formatted("DatasetVersion", datasetVersionDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE, Role.STR_DATASET_READ,
            Role.STR_SEARCH })
    public void deleteDatasetWithoutDatasetVersion_shouldSucceed() {
        initDataset(DatasetType.CLOSED);

        datasetService.deleteDataset(datasetId);

        assertThatThrownBy(() -> datasetService.getById(datasetId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessage("%s : %s inexistant.".formatted("Dataset", datasetId));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE, Role.STR_DATASET_READ,
            Role.STR_SEARCH })
    public void createDatasetVersion() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        saveDatasetVersion();

        given()
                .pathParam("datasetVersionId", datasetVersionDto.getId())
                .contentType(ContentType.JSON)
                .when()
                .get("/dataset-versions/id/{datasetVersionId}")
                .then()
                .body("producer", is("author"))
                .body("additionalInformation", is("SomeInformations"))
                .body("productionDate", is(fixedDate.toString()));
    }

}
