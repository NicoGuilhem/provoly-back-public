package com.provoly.ref.dataset;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDto;
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
    private DatasetVersionDto datasetVersionDto;
    private OClass oClass;
    private Dataset dataset;
    private MetadataDefDto metadataDefDto;
    private static final String OCLASS_ID = "bdcdf6f7-5ab4-4b79-b521-7d33675186f4";

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
                DatasetState.ACTIVE, withFile);
    }

    private void saveDatasetVersion() {
        DatasetVersion datasetVersion = datasetVersionMapper.toModel(datasetVersionDto);
        datasetVersionService.createDatasetVersion(datasetVersion);
    }

    private void saveCloseDatasetWithDatasetState(DatasetState wantedState) {
        switch (wantedState) {
            case LOADING -> saveCloseDatasetAtLoading();
            case INDEXING -> saveCloseDatasetAtIndexing();
            case ACTIVE -> saveCloseDatasetAtActive();
            case INACTIVE -> saveCloseDatasetAtInactive();
            case ERROR -> saveCloseDatasetAtError();
        }
    }

    private void saveCloseDatasetAtLoading() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        saveDatasetVersion();
    }

    private void saveCloseDatasetAtIndexing() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
    }

    private void saveCloseDatasetAtActive() {
        saveCloseDatasetAtIndexing();
        datasetVersionController.activate(datasetVersionId);
    }

    private void saveCloseDatasetAtError() {
        saveCloseDatasetAtIndexing();
        DatasetVersionDto error = generateDatasetDtoWithState(DatasetState.ERROR);
        datasetVersionController.updateState(error);

    }

    private void saveCloseDatasetAtInactive() {
        saveCloseDatasetAtActive();
        //TODO: remplacer par un appel a l'endpoint de désactivation une fois codé
        DatasetVersionDto inactive = generateDatasetDtoWithState(DatasetState.INACTIVE);
        datasetVersionController.updateState(inactive);
    }

    private DatasetVersionDto generateDatasetDtoWithState(Boolean withFile, DatasetState state) {
        return new DatasetVersionDto(datasetVersionId, datasetId, oClass.getId(), state, withFile);
    }

    private DatasetVersionDto generateDatasetDtoWithState(DatasetState state) {
        return generateDatasetDtoWithState(false, state);
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
    public void updateDatasetVersion_returnKO() {
        initDataset(DatasetType.MODIFIABLE);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setName("Casséééééééés");
        dataset.setoClass(oClass);
        dataset.setSlug(oClass.getSlug());
        dataset.setType(DatasetType.CLOSED);
        dataset.setUser(userService.getCurrentUser());
        datasetService.saveEntity(dataset);
        var datasetVersionDto = new DatasetVersionDto(this.datasetVersionDto.getId(), dataset.getId(), this.oClass.getId(),
                this.datasetVersionDto.getVersion());

        assertThatThrownBy(() -> datasetVersionController.updateState(datasetVersionDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Dataset versions are immutable.");

        datasetService.deleteDataset(dataset.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_WRITE, Role.STR_ITEM_WRITE })
    public void deleteDatasetVersion_returnOk() {
        initDataset(DatasetType.MODIFIABLE);
        generateDatasetVersionDto(false);
        saveDatasetVersion();
        String datasetId = datasetVersionDto.getId().toString();
        given()
                .pathParam("datasetVersionId", datasetVersionDto.getId())
                .contentType(ContentType.JSON)
                .when()
                .delete("/dataset-versions/id/{datasetVersionId}")
                .then()
                .statusCode(200);
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
                DatasetState.ACTIVE, false);

        // WHEN
        datasetVersionController.create(secondDatasetVersion);
        assertThat(datasetVersionController.get(secondDatasetVersion.getId()).getVersion()).isEqualTo(2);

        var maxVersion = datasetVersionService.getAllByDatasetId(datasetId).stream()
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
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateLoading_when_updateWithStateIndexing_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.LOADING);
        DatasetVersionDto datasetVersionDtoIndexing = generateDatasetDtoWithState(
                datasetVersionController.get(datasetVersionId).isWithFile(), DatasetState.INDEXING);

        datasetVersionService.updateState(datasetVersionMapper.toModel(datasetVersionDtoIndexing));
        assertThat(datasetVersionController.get(datasetVersionDto.getId()).getState()).isEqualTo(DatasetState.INDEXING);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateLoading_when_updateWithStateError_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.LOADING);
        DatasetVersionDto datasetVersionDtoError = generateDatasetDtoWithState(true, DatasetState.ERROR);

        datasetVersionService.updateState(datasetVersionMapper.toModel(datasetVersionDtoError));
        assertThat(datasetVersionController.get(datasetVersionDto.getId()).getState()).isEqualTo(DatasetState.ERROR);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateLoading_when_updateWithStateActive_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.LOADING);
        DatasetVersionDto datasetVersionDtoActive = generateDatasetDtoWithState(true, DatasetState.ACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoActive)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateLoading_when_updateWithStateInactive_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.LOADING);
        DatasetVersionDto datasetVersionDtoInactive = generateDatasetDtoWithState(true, DatasetState.INACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoInactive)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateIndexing_when_updateWithStateActive_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.INDEXING);
        DatasetVersionDto datasetVersionDtoActive = generateDatasetDtoWithState(false, DatasetState.ACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoActive)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateIndexing_when_updateWithStateError_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.INDEXING);
        DatasetVersionDto datasetVersionDtoError = generateDatasetDtoWithState(false, DatasetState.ERROR);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoError)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateIndexing_when_updateWithStateLoading_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.INDEXING);
        DatasetVersionDto datasetVersionDtoLoading = generateDatasetDtoWithState(false, DatasetState.LOADING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoLoading)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionStateIndexing_when_updateWithStateInactive_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.INDEXING);
        DatasetVersionDto datasetVersionDtoIndexing = generateDatasetDtoWithState(false, DatasetState.INACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoIndexing)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void put_closedDatasetVersionActive_when_updateWithInactiveState_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.ACTIVE);
        DatasetVersionDto datasetVersionDtoInactive = generateDatasetDtoWithState(false, DatasetState.INACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoInactive)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void put_closedDatasetVersionActive_when_updateWithLoadingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ACTIVE);
        DatasetVersionDto datasetVersionDtoLoading = generateDatasetDtoWithState(false, DatasetState.LOADING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoLoading)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE, Role.STR_DATASET_READ })
    public void put_closedDatasetVersionActive_when_updateWithIndexingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ACTIVE);
        DatasetVersionDto datasetVersionDtoIndexing = generateDatasetDtoWithState(false, DatasetState.INDEXING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoIndexing)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_READ, Role.STR_DATASET_WRITE })
    public void put_closedDatasetVersionActive_when_updateWithErrorState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ACTIVE);
        DatasetVersionDto datasetVersionDtoError = generateDatasetDtoWithState(false, DatasetState.ERROR);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoError)
                .when()
                .put("/datasets")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void put_closedDatasetVersionInactive_when_updateWithLoadingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.INACTIVE);
        DatasetVersionDto datasetVersionDtoLoading = generateDatasetDtoWithState(false, DatasetState.LOADING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoLoading)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void put_closedDatasetVersionInactive_when_updateWithIndexingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.INACTIVE);
        DatasetVersionDto datasetVersionDtoIndexing = generateDatasetDtoWithState(false, DatasetState.INDEXING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoIndexing)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_DATASET_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_READ, Role.STR_ITEM_WRITE })
    public void put_closedDatasetVersionInactive_when_updateWithActiveState_should_returnOK() {
        saveCloseDatasetWithDatasetState(DatasetState.INACTIVE);
        DatasetVersionDto datasetVersionDtoIndexing = generateDatasetDtoWithState(false, DatasetState.ACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoIndexing)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH, Role.STR_DATASET_WRITE, Role.STR_DATASET_READ })
    public void put_closedDatasetVersionInactive_when_updateWithErrorState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.INACTIVE);
        DatasetVersionDto datasetVersionDtoError = generateDatasetDtoWithState(false, DatasetState.ERROR);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoError)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionError_when_updateWithActiveState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ERROR);
        DatasetVersionDto datasetVersionDtoUpdate = generateDatasetDtoWithState(false, DatasetState.ACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoUpdate)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionError_when_updateWithLoadingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ERROR);
        DatasetVersionDto datasetVersionDtoUpdate = generateDatasetDtoWithState(false, DatasetState.LOADING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoUpdate)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionError_when_updateWithIndexingState_should_throwBadRequest() {
        saveCloseDatasetWithDatasetState(DatasetState.ERROR);
        DatasetVersionDto datasetVersionDtoUpdate = generateDatasetDtoWithState(false, DatasetState.INDEXING);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoUpdate)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_ITEM_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void put_closedDatasetVersionError_when_updateWithInactiveState_should_throwBadRequest() {
        saveCloseDatasetAtError();
        DatasetVersionDto datasetVersionDtoUpdate = generateDatasetDtoWithState(false, DatasetState.INACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDtoUpdate)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void put_updateClosedDataset_when_gaveSameState_throwBadRequest() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(false);
        saveDatasetVersion();

        given()
                .contentType(ContentType.JSON)
                .body(datasetVersionDto)
                .when()
                .put("/dataset-versions")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void deactivateDataset_should_setStateAtInactive() {
        saveCloseDatasetAtActive();
        datasetVersionService.deactivateDatasetVersion(datasetVersionId);
        assertThat(datasetVersionService.getById(datasetVersionId).getState()).isEqualTo(DatasetState.INACTIVE);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void importDataset_when_anotherOneIsImporting_should_ReturnOK() {
        saveCloseDatasetAtLoading();

        DatasetDto anotherDatasetDto = new DatasetDto(UUID.randomUUID(), "another one", oClass.getId(), DatasetType.CLOSED);
        datasetService.save(anotherDatasetDto);
        DatasetVersionDto anotherDatasetVersion = new DatasetVersionDto(UUID.randomUUID(), anotherDatasetDto.getId(),
                oClass.getId(), DatasetState.LOADING);

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
        saveCloseDatasetAtLoading();

        DatasetVersionDto anotherDatasetVersionWithSameDataset = new DatasetVersionDto(UUID.randomUUID(), datasetId,
                oClass.getId(), DatasetState.LOADING);

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
        generateDatasetVersionDto(true);
        saveDatasetVersion();
        var dsVersion = new DatasetVersionDto(datasetVersionId, datasetId, oClass.getId(),
                DatasetState.ERROR, true);
        datasetVersionController.updateState(dsVersion);

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

        datasetVersionMessageService.save(new ImportsMessage(dsVersion.getId(), "1", errors));
        datasetVersionMessageService.save(new ImportsMessage(dsVersion.getId(), null, warnings));

        // WHEN
        var datasetVersionPreview = datasetVersionController.getDatasetVersionPreviews(dsVersion.getId());

        // THEN
        assertThat(datasetVersionPreview.get(0).count()).isEqualTo(8);
        assertThat(datasetVersionPreview.get(1).count()).isEqualTo(6);
        assertThat(datasetVersionPreview.get(0).messages()).hasSize(DatasetVersionService.PREVIEW_MAX_RESULT);
        assertThat(datasetVersionPreview.get(1).messages()).hasSize(DatasetVersionService.PREVIEW_MAX_RESULT);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_READ })
    public void get_DatasetVersionError_should_returnEmpty() {
        initDataset(DatasetType.CLOSED);
        generateDatasetVersionDto(true);
        saveDatasetVersion();
        var dsVersion = new DatasetVersionDto(datasetVersionId, datasetId, oClass.getId(),
                DatasetState.ERROR, true);
        datasetVersionController.updateState(dsVersion);

        var datasetVersionPreview = datasetVersionController.getDatasetVersionPreviews(dsVersion.getId());
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
        saveCloseDatasetAtActive();

        var result = datasetVersionService.getAllActiveForClass(UUID.fromString(OCLASS_ID));

        assertThat(result).hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE })
    public void getZeroDatasetVersionOfADataset_shouldThrow() {
        initDataset(DatasetType.CLOSED);

        assertThatThrownBy(() -> datasetVersionService.getByDatasetId(datasetId))
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

}
