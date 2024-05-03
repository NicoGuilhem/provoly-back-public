package com.provoly.ref.dataset;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.dataset.*;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.VisibilityDto;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.DashboardService;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.groups.GroupErrors;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.groups.GroupWrite;
import com.provoly.ref.metadata.MetadataDefController;
import com.provoly.ref.model.*;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.utils.TestService;
import com.provoly.ref.widget.WidgetService;
import com.provoly.ref.widget.dto.WidgetDto;
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
public class DatasetControllerTest {

    @Inject
    ModelService modelService;
    @Inject
    ModelController modelController;
    @Inject
    ModelMapper modelMapper;
    @Inject
    DatasetService datasetService;
    @Inject
    DatasetRepository datasetRepository;
    @Inject
    DatasetController datasetController;
    @Inject
    DatasetVersionService datasetVersionService;
    @Inject
    DatasetVersionMapper datasetVersionMapper;
    @Inject
    DatasetMapper datasetMapper;
    @Inject
    MetadataDefController metadataDefController;
    @Inject
    TestService testService;
    @Inject
    DashboardService dashboardService;
    @Inject
    WidgetService widgetService;
    @Inject
    GroupService groupService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    UserService userService;
    private DatasetDto datasetDto;
    private DatasetVersionDto datasetVersionDto;
    private MetadataDefDto metadataDefDto;

    private final UUID attributeId = UUID.randomUUID();
    private OClassWriteDto oClass = null;
    private FieldDto fieldDto = null;

    @BeforeEach
    public void init() throws IOException {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        fieldDto = testService.createAndSaveField();
        AttributeDefDto attributeDefDto = testService.createAttributeDto(attributeId, "attributeName",
                "attributeId" + attributeId,
                fieldDto.id);
        oClass = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(oClass));
        if (!modelService.exists(modelMapper.toModel(oClass))) {
            modelService.saveEntity(modelMapper.toModel(oClass));
        }

        // add dataset
        datasetDto = new DatasetDto(
                UUID.fromString("999aa06c-9204-4cc5-849b-7d348316bec6"),
                "c@r@ctér`st`qu€",
                oClass.getId(),
                DatasetType.MODIFIABLE);
        datasetVersionDto = new DatasetVersionDto(UUID.fromString("999aa06c-9204-4cc5-849b-7d348316bec6"),
                datasetDto.getId(), oClass.getId(), DatasetState.ACTIVE);

        ProvolyUser currentUser = userService.getCurrentUser();
        Dataset dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(currentUser);
        datasetRepository.save(dataset);
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersionDto));
        try {
            groupService.addGroup(new GroupWrite(UUID.randomUUID(), "new_group"));
            groupService.addGroup(new GroupWrite(UUID.randomUUID(), "test_group"));
        } catch (BusinessException e) {
            // skipping group creation as it already exist
        }

    }

    @AfterEach
    @Transactional
    public void delete() {
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "data-reader" })
    public void role_user_isUnauthorized() {
        given()
                .pathParam("id", datasetDto.getId())
                .contentType(ContentType.JSON)
                .when()
                .delete("/datasets/id/{id}")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_WRITE })
    public void deleteUnknownDataset() {
        String datasetId = "00000000-0000-0000-0000-000000000000";
        given()
                .pathParam("id", datasetId)
                .contentType(ContentType.JSON)
                .when()
                .delete("/datasets/id/{id}")
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Dataset : %s inexistant.".formatted(datasetId)));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void updateDataset_type_returnKO() {
        var dataset = new DatasetDto(datasetDto.getId(), datasetDto.getName(), datasetDto.getoClass(),
                DatasetType.CLOSED);
        given()
                .body(dataset)
                .contentType(ContentType.JSON)
                .when()
                .put("/datasets")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE })
    public void updateDataset_class_returnKO() {
        // add new class
        oClass = new OClassWriteDto(UUID.randomUUID(), "newClassTest", new ArrayList<>(), Storage.ELASTIC);
        if (!modelService.exists(modelMapper.toModel(oClass))) {
            modelService.saveEntity(modelMapper.toModel(oClass));
        }
        var dataset = new DatasetDto(datasetDto.getId(), datasetDto.getName(), oClass.getId(),
                DatasetType.MODIFIABLE);
        given()
                .body(dataset)
                .contentType(ContentType.JSON)
                .when()
                .put("/datasets")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_READ })
    public void getDatasetVersionOfDataset_returnLastActiveDatasetVersion() {
        // GIVEN
        ProvolyUser currentUser = userService.getCurrentUser();
        var datasetDto = new DatasetDto(
                UUID.randomUUID(),
                "Closed_dataset",
                oClass.getId(),
                DatasetType.CLOSED);
        var dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(currentUser);
        datasetRepository.save(dataset);

        var activeDatasetVersionDto = new DatasetVersionDto(UUID.randomUUID(), datasetDto.getId(), oClass.getId(),
                DatasetState.ACTIVE, "author", Instant.now());
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(activeDatasetVersionDto));
        datasetVersionService.activateDatasetVersion(activeDatasetVersionDto.getId());

        var errorDatasetVersion = new DatasetVersionDto(UUID.randomUUID(), datasetDto.getId(), oClass.getId(),
                DatasetState.ERROR, "author", Instant.now());
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(errorDatasetVersion));

        // WHEN
        var datasetVersion = datasetController.getDatasetVersionByDatasetId(datasetDto.getId());

        // THEN
        assertThat(datasetVersion.getVersion()).isEqualTo(1);
        assertThat(datasetVersion.getState()).isEqualTo(DatasetState.ACTIVE);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_READ })
    public void getLastDatasetVersionOfDataset_returnLastDatasetVersion() {
        // GIVEN
        ProvolyUser currentUser = userService.getCurrentUser();
        var datasetDto = new DatasetDto(
                UUID.randomUUID(),
                "Closed_dataset",
                oClass.getId(),
                DatasetType.CLOSED);
        var dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(currentUser);
        datasetRepository.save(dataset);

        var activeDatasetVersionDto = new DatasetVersionDto(UUID.randomUUID(), datasetDto.getId(), oClass.getId(),
                DatasetState.ACTIVE, "producer", Instant.now());
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(activeDatasetVersionDto));

        // WHEN
        var datasetVersion = datasetController.getLastVersionCreated(datasetDto.getId());

        // THEN
        assertThat(datasetVersion.getVersion()).isEqualTo(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_READ })
    public void getLastDatasetVersionOfUnknownDataset_shouldThrowError() {
        var datasetRandom = UUID.randomUUID();

        // THEN
        assertThatThrownBy(() -> datasetController.getLastVersionCreated(datasetRandom))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessage("Dataset : %s inexistant.".formatted(datasetRandom));
    }

    @Test
    public void generateDatasetSlug() {
        Dataset model = datasetMapper.toModel(datasetDto);
        assertThat(model.getSlug()).endsWith("_crcterstqu");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_ITEM_WRITE })
    public void shouldGetDatasetCountByClass() {
        var result = modelController.getDatasetCountByClass();

        assertThat(result).isNotNull().isEqualTo(List.of(new CountDto(oClass.getId(), 1)));
    }

    private void generateMetadataDefWithType(VariableType type, String description) {
        metadataDefDto = new MetadataDefDto();
        metadataDefDto.id = UUID.fromString("32c52b41-b197-4e57-8f6b-a8bf53c9c167");
        metadataDefDto.name = UUID.randomUUID().toString();
        metadataDefDto.type = type;
        metadataDefDto.description = description;
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_WRITE, Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_DATASET_READ,
            Role.STR_SEARCH })
    public void should_add_metadata_to_dataset() {
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);
        var metadataValueWrite = new MetadataValueWriteDto();
        metadataValueWrite.setValue("12");

        datasetController.setMetadata(datasetVersionDto.getId(), metadataDefDto.id, metadataValueWrite);

        assertThat(datasetController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetDetailsDto.class)
                .extracting(DatasetDetailsDto::getMetadata)
                .extracting(x -> x.get(0).getValue())
                .isEqualTo("12");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_WRITE, Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_DATASET_READ,
            Role.STR_SEARCH })
    public void should_add_group_to_dataset() {
        assertThat(datasetController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetDetailsDto.class)
                .extracting(DatasetDetailsDto::getGroups)
                .extracting(List::size)
                .isEqualTo(0);

        datasetDto.setGroups(List.of("new_group"));
        datasetController.update(datasetDto);

        assertThat(datasetController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetDetailsDto.class)
                .extracting(DatasetDetailsDto::getGroups)
                .extracting(List::getFirst)
                .isEqualTo("new_group");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASET_WRITE })
    public void should_delete_metadata_of_dataset() {
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);
        var metadataValueWrite = new MetadataValueWriteDto();
        metadataValueWrite.setValue("12");

        datasetController.setMetadata(datasetVersionDto.getId(), metadataDefDto.id, metadataValueWrite);

        datasetController.deleteMetadata(datasetVersionDto.getId(), metadataDefDto.id);

        assertThat(datasetController.get(datasetVersionDto.getId()))
                .isInstanceOf(DatasetDetailsDto.class)
                .extracting(DatasetDetailsDto::getMetadata)
                .isEqualTo(List.of());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_DATASET_WRITE, Role.STR_CLASS_READ,
            Role.STR_SEARCH })
    public void delete_inexistant_metadata_shouldThrowError() {
        generateMetadataDefWithType(VariableType.INTEGER, "description");
        metadataDefController.addMetadata(metadataDefDto);

        assertThatThrownBy(() -> datasetController.deleteMetadata(datasetVersionDto.getId(), metadataDefDto.id))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Metadata %s is not assigned to dataset %s".formatted(metadataDefDto.id,
                                datasetDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfDatasetReferencedByDashboard() {
        UUID dashboardId = UUID.randomUUID();
        createDashboardDto(dashboardId);

        var result = datasetService.getDatasetAssociations(datasetDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(dashboardId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfDatasetReferencedByWidget() {
        UUID widgetId = UUID.randomUUID();
        createWidgetDto(widgetId);

        var result = datasetService.getDatasetAssociations(datasetDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(widgetId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_datasetWithDescription() {
        UUID datasetId = UUID.randomUUID();
        Dataset dataset = createDataset(datasetId, "description", "name");

        var result = datasetService.getById(datasetId);
        assertThat(result).isEqualTo(dataset);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_datasetWithNoDescription() {
        UUID datasetId = UUID.randomUUID();
        Dataset dataset = createDataset(datasetId, null, "name");

        var result = datasetService.getById(datasetId);
        assertThat(result).isEqualTo(dataset);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATASET_READ })
    public void deleteDatasetWhoOwnsDatasetVersion_shouldThrow() {
        // GIVEN
        ProvolyUser provolyUser = userService.getCurrentUser();
        var datasetDto = new DatasetDto(
                UUID.randomUUID(),
                "Closed_dataset",
                oClass.getId(),
                DatasetType.CLOSED);
        var dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetRepository.save(dataset);

        var activeDatasetVersionDto = new DatasetVersionDto(UUID.randomUUID(), datasetDto.getId(), oClass.getId(),
                DatasetState.ACTIVE, "author", Instant.now());
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(activeDatasetVersionDto));
        datasetVersionService.activateDatasetVersion(activeDatasetVersionDto.getId());

        var errorDatasetVersion = new DatasetVersionDto(UUID.randomUUID(), datasetDto.getId(), oClass.getId(),
                DatasetState.ERROR, "author", Instant.now());
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(errorDatasetVersion));

        // THEN
        assertThatThrownBy(() -> datasetService.deleteDataset(datasetDto.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You're not allowed to delete dataset %s because it owns one or more dataset version"
                        .formatted(datasetDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void getDatasetByName_shouldSucceed() {
        var result = datasetService.getByName("c@r@ctér`st`qu€");

        assertThat(result).extracting(EntityId::getId).isEqualTo(datasetDto.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void getUnknownDatasetByName_shouldThrow() {
        String datasetName = "je suis un nom random";

        assertThatThrownBy(() -> datasetService.getByName(datasetName))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Dataset %s doesn't exists".formatted(datasetName));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void getDatasetByIfOfIamsperadmin_shouldNotSucceed() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();
        createDataset(datasetId, "description", "name");

        testService.authenticate("iampolice", currentSubjectProvider);
        assertThatThrownBy(() -> datasetController.get(datasetId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessage("Dataset : %s inexistant.".formatted(datasetId));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH, Role.STR_DATASET_READ })
    public void getDatasetByNameOfIamsperadmin_shouldNotSucceed() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();
        Dataset dataset = createDataset(datasetId, "description", "name");

        testService.authenticate("iampolice", currentSubjectProvider);
        assertThatThrownBy(() -> datasetController.getByName(dataset.getName()))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessage("Dataset %s doesn't exists".formatted(dataset.getName()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void getAllDatasetOfClass_shouldOnlyGet1() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        createDataset(UUID.randomUUID(), "description", "name");

        testService.authenticate("iampolice", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();
        createDataset(datasetId, "description", "name" + UUID.randomUUID());
        var result = datasetController.getAllForClass(oClass.getId());

        assertThat(result).extracting(DatasetDetailsDto::getId)
                .isEqualTo(List.of(datasetId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void addGroupToDataset_shouldSucceed() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();
        createDataset(datasetId, "name", Set.of("new_group"));

        var result = datasetController.get(datasetId);
        assertThat(result).extracting(DatasetDetailsDto::getGroups).isEqualTo(List.of("new_group"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void updateDatasetWithGroupNull_shouldNotModifyGroup() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();

        createDataset(datasetId, "name", Set.of("new_group"));
        updateGroup(datasetId, "name", null);

        var result = datasetController.get(datasetId);
        assertThat(result).extracting(DatasetDetailsDto::getGroups).isEqualTo(List.of("new_group"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void updateDatasetWithGroupEmpty_shouldCleanGroups() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();

        createDataset(datasetId, "name", Set.of("new_group"));
        updateGroup(datasetId, "name", Set.of());

        var result = datasetController.get(datasetId);
        assertThat(result).extracting(DatasetDetailsDto::getGroups).isEqualTo(List.of());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void addingGroupOfDatasetNotDefineByDashboard_shouldReturnGroupList() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        Set<String> groups = Set.of("new_group");
        UUID dashboardId = UUID.randomUUID();
        var expected = new GroupErrors(Map.of(dashboardId, Set.of("test_group")));

        createDataset(datasetDto.getId(), "name", groups);
        createDashboardDto(dashboardId, Set.of("test_group"));

        var result = updateGroup(datasetDto.getId(), "name", Set.of());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void addingGroupAuthenticatedWithDashboardWithGroupALL_shouldReturnGroupList() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        Set<String> groups = Set.of("ALL");
        UUID dashboardId = UUID.randomUUID();
        var expected = new GroupErrors(Map.of(dashboardId, groups));

        createDataset(datasetDto.getId(), "name", Set.of("AUTHENTICATED"));
        createDashboardDto(dashboardId, groups);

        var result = updateGroup(datasetDto.getId(), "name", Set.of());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void updateDatasetALLWithGroupNewGroup_shouldSucceed() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();

        createDataset(datasetId, "name", Set.of("ALL"));
        updateGroup(datasetId, "name", Set.of("new_group"));

        var result = datasetController.get(datasetId);

        assertThat(result).extracting(DatasetDetailsDto::getGroups).isEqualTo(List.of("new_group"));

    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void addingDatasetWithEmptyGroup_shouldReturnDashboardWithoutEmptyGroup() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        Set<String> groups = Set.of("new_group");
        UUID dashboardId = UUID.randomUUID();
        var expected = new GroupErrors(Map.of(dashboardId, groups));

        createDataset(datasetDto.getId(), "name", Set.of());
        createDashboardDto(dashboardId, groups);

        var result = updateGroup(datasetDto.getId(), "name", Set.of());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public void createDatasetWhoAlreadyExist_shouldThrow() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();

        createDataset(UUID.randomUUID(), "name", Set.of("new_group"));

        assertThatThrownBy(() -> createDataset(datasetId, "name", Set.of("new_group")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Name %s already exists for class %s".formatted("name", oClass.getName()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_ITEM_WRITE, Role.STR_SEARCH, Role.STR_DATASET_WRITE })
    public void updateImmutableDataset_shouldThrow() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID datasetId = UUID.randomUUID();
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), "attributeName",
                "attributeId" + attributeId,
                fieldDto.id);
        UUID classId = UUID.randomUUID();
        OClassWriteDto oClassWriteDto = testService.createClassWriteDto(classId, "classDto", attributeDefDto);

        modelService.saveEntity(modelMapper.toModel(oClassWriteDto));
        createDataset(datasetId, "name", Set.of());

        assertThatThrownBy(
                () -> datasetController.update(new DatasetDto(datasetId, "name", classId, DatasetType.CLOSED, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The dataset's type is immutable.");
    }

    private GroupErrors updateGroup(UUID datasetId, String name, Set<String> groups) {
        DatasetDto dataset = new DatasetDto(datasetId, name, oClass.getId(), DatasetType.CLOSED,
                groups == null ? null : groups);
        return datasetService.updateDataset(dataset);
    }

    private Dataset createDataset(UUID datasetId, String name, Set<String> groups) {
        DatasetDto dataset = new DatasetDto(datasetId, name, oClass.getId(), DatasetType.CLOSED, groups);
        datasetService.save(dataset);
        return datasetService.findById(datasetId);
    }

    private Dataset createDataset(UUID datasetId, String description, String name) {
        DatasetDto dataset = new DatasetDto(datasetId, name, oClass.getId(), DatasetType.CLOSED, description);
        datasetService.save(dataset);
        return datasetService.findById(datasetId);
    }

    private void createWidgetDto(UUID widgetId) {
        WidgetDto widgetDto = new WidgetDto();
        widgetDto.id = widgetId;
        widgetDto.name = "widget";
        widgetDto.datasource = List.of(datasetDto.getId());
        widgetDto.visibility = new VisibilityDto(VisibilityType.PUBLIC.name(), List.of());
        widgetDto.description = "";
        widgetDto.content = "";
        widgetDto.cover = true;
        widgetDto.image = "";
        widgetService.addWidget(widgetDto);
    }

    private void createDashboardDto(UUID dashboardId, Set<String> groups) {
        DashboardWriteDto dashboardDto = new DashboardWriteDto(dashboardId, "Dashboard test", "image",
                "description", false, List.of(datasetDto.getId()), null, null,
                groups.stream().collect(Collectors.toMap(g -> g, g -> List.of(GroupRights.READ))));
        dashboardService.saveOrUpdate(dashboardDto);
    }

    private void createDashboardDto(UUID dashboardId) {
        DashboardWriteDto dashboardDto = new DashboardWriteDto(dashboardId, "Dashboard test", "image",
                "description", false, List.of(datasetDto.getId()), null, null, Map.of());
        dashboardService.saveOrUpdate(dashboardDto);
    }
}