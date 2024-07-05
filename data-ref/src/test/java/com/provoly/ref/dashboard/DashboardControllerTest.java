package com.provoly.ref.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.VariableType;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DashboardControllerTest {

    private static final String DASHBOARD_NAME = "My dashboard";
    private final UUID privateId = UUID.randomUUID();
    private final UUID publicId = UUID.randomUUID();
    @Inject
    DashboardController dashboardController;
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    DatasetService datasetService;

    private UUID dashboardId;
    private DashboardWriteDto dashboardToSave;
    private DashboardWriteDto privateDashboard;

    private DashboardWriteDto publicDashboard;

    @Inject
    TestService testService;
    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetadataService metadataService;
    @Inject
    MetadataMapper metadataMapper;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;

    @BeforeEach
    public void authenticate() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
    }

    @AfterEach
    @Transactional
    public void deleteDashboards() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.clean();
    }

    void initDashboard() {
        dashboardId = UUID.randomUUID();
        dashboardToSave = new DashboardWriteDto(dashboardId, DASHBOARD_NAME, "image", "description", false,
                List.of(), Map.of("type", "private"), null, Map.of("ALL", List.of(GroupRights.READ)), "some more information");
        dashboardController.saveDashboard(dashboardToSave);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    void shouldSaveDashboard() {
        initDashboard();

        Collection<DashboardReadDto> dashboards = dashboardController.getAllForCurrentUser();
        assertThat(dashboards).extracting("id").contains(dashboardId);

        DashboardReadDto dashboard = ((List<DashboardReadDto>) dashboards).get(0);

        assertThat(dashboard.getAdditionalInformation()).isEqualTo("some more information");
        assertThat(dashboard.getName()).isEqualTo(DASHBOARD_NAME);
        assertThat(dashboard.getDescription()).isEqualTo("description");
        assertThat(dashboard.getCover()).isFalse();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    void shouldUpdateDashboard() {
        initDashboard();

        DashboardReadDto savedDashboard = retrieveSavedOrUpdatedDashboard();
        assertThat(savedDashboard.getCreationDate()).isNotNull();
        assertThat(savedDashboard.getModificationDate()).isNotNull();

        //update
        DatasetDto datasetDto = initDataset();
        DashboardWriteDto dashboardToUpdate = new DashboardWriteDto(dashboardId, DASHBOARD_NAME, List.of(datasetDto.getId()),
                "Some new information");
        dashboardController.saveDashboard(dashboardToUpdate);

        DashboardReadDto updatedDashboard = retrieveSavedOrUpdatedDashboard();
        assertThat(updatedDashboard.getCreationDate()).isEqualTo(savedDashboard.getCreationDate());
        assertThat(updatedDashboard.getModificationDate()).isAfter(savedDashboard.getModificationDate());
        assertThat(updatedDashboard.getDatasource()).containsExactly(datasetDto.getId());
        assertThat(updatedDashboard.getAdditionalInformation()).isEqualTo("Some new information");
    }

    private DashboardReadDto retrieveSavedOrUpdatedDashboard() {
        Collection<DashboardReadDto> savedDashboards = dashboardController.getAllForCurrentUser();
        var savedDashboardId = savedDashboards.stream()
                .filter(d -> d.getId().equals(dashboardId))
                .findFirst()
                .map(DashboardReadDto::getId)
                .get();
        return dashboardController.getById(savedDashboardId);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_not_get_widget_forbidden() {
        createDashboards();

        testService.authenticate("iampolice", currentSubjectProvider);
        assertThatThrownBy(() -> dashboardController.getDashBoardManifest(privateId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dashboard : %s inexistant.".formatted(privateId));

    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_get_public_dashboard() {
        createDashboards();

        testService.authenticate("iampolice", currentSubjectProvider);

        var res = dashboardController.getDashBoardManifest(publicId);
        assertThat(res).isEqualTo(publicDashboard.getManifest());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_addMetadataOnDashboard() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);
        createDashboards();

        metadataService.addMetadataToEntity(privateId, metadataDefDto.id, metadataValueWriteDto, EntityType.DASHBOARD);

        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(metadataValue -> metadataValue.getMetadataDef().id)
                .isEqualTo(List.of(metadataDefDto.id));
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_deleteMetadataOnDashboard() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);
        createDashboards();

        metadataService.addMetadataToEntity(privateId, metadataDefDto.id, metadataValueWriteDto, EntityType.DASHBOARD);
        metadataService.deleteMetadataValueByEntityId(privateId, metadataDefDto.id, EntityType.DASHBOARD);

        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of());
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_addMetadataOnDashboardAtDashboardCreation() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);
        createDashboardsWithMetadata(List.of(metadataValueWriteDto));

        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto));
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_updateMetadataOnDashboardAtDashboardCreation() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataDefDto secondmetadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);
        MetadataValueWriteDto secondMetadataValueWriteDto = createMetadata(secondmetadataDefDto.id);

        createDashboardsWithMetadata(List.of(metadataValueWriteDto));
        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto));

        createDashboardsWithMetadata(List.of(metadataValueWriteDto, secondMetadataValueWriteDto));
        var result = dashboardController.getById(privateId);
        assertThat(result.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto, secondmetadataDefDto));
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_updateMetadataWithEmptyTabOnDashboard() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);

        createDashboardsWithMetadata(List.of(metadataValueWriteDto));
        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto));

        createDashboardsWithMetadata(List.of());
        var result = dashboardController.getById(privateId);
        assertThat(result.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEmpty();
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_updateMetadataWithNullTabOnDashboard() {
        MetadataDefDto metadataDefDto = createMetadataDef();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(metadataDefDto.id);

        createDashboardsWithMetadata(List.of(metadataValueWriteDto));
        var res = dashboardController.getById(privateId);
        assertThat(res.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto));

        createDashboardsWithMetadata(List.of(metadataValueWriteDto));
        var result = dashboardController.getById(privateId);
        assertThat(result.getMetadata())
                .extracting(MetadataValueReadDto::getMetadataDef)
                .isEqualTo(List.of(metadataDefDto));
    }

    @Test
    @Transactional
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void should_updateMetadataWithUnknownMetadataOnDashboard() {
        UUID randomMetadataDefId = UUID.randomUUID();
        MetadataValueWriteDto metadataValueWriteDto = createMetadata(randomMetadataDefId);
        List<MetadataValueWriteDto> metadatas = List.of(metadataValueWriteDto);

        assertThatThrownBy(() -> createDashboardsWithMetadata(metadatas))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MetadataDef : %s inexistant.".formatted(randomMetadataDefId));
    }

    private MetadataDefDto createMetadataDef() {
        MetadataDefDto metadataDefDto = new MetadataDefDto();
        metadataDefDto.id = UUID.randomUUID();
        metadataDefDto.name = "metadataDef" + metadataDefDto.id;
        metadataDefDto.description = "description";
        metadataDefDto.type = VariableType.STRING;
        metadataDefDto.slug = "slug";
        metadataDefDto.allowedValues = null;
        metadataDefService.addMetadata(metadataMapper.toModel(metadataDefDto));
        return metadataDefDto;
    }

    private MetadataValueWriteDto createMetadata(UUID metadataDefId) {
        MetadataValueWriteDto metadataValueWriteDto = new MetadataValueWriteDto();
        metadataValueWriteDto.setMetadataDefId(metadataDefId);
        metadataValueWriteDto.setValue("12");
        return metadataValueWriteDto;
    }

    private void createDashboards() {
        DatasetDto datasetDto = initDataset();

        privateDashboard = new DashboardWriteDto(privateId, "private", "image", "description", false,
                List.of(), Map.of("type", "private"), null, Map.of(), "Some more information");
        dashboardController.saveDashboard(privateDashboard);

        publicDashboard = new DashboardWriteDto(publicId, "public", "image", "description", false,
                List.of(datasetDto.getId()), Map.of("type", "public"), null, Map.of("ALL", List.of(GroupRights.READ)),
                "Some more information");
        dashboardController.saveDashboard(publicDashboard);
    }

    private void createDashboardsWithMetadata(List<MetadataValueWriteDto> metadataValueWriteDto) {
        privateDashboard = new DashboardWriteDto(privateId, "private", "image", "description", false,
                List.of(), Map.of("type", "private"), metadataValueWriteDto, Map.of(), "Some more information");
        dashboardController.saveDashboard(privateDashboard);
    }

    private DatasetDto initDataset() {
        UUID fieldId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        FieldDto fieldDto = testService.createAndSaveField(fieldId);
        AttributeDefDto attributeDefDto = testService.createAttributeDto(attributeId, "attributeName",
                "attributeId" + attributeId, fieldDto);
        OClassWriteDto classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));

        DatasetDto dto = new DatasetDto(UUID.randomUUID(), "test", classDto.getId(), DatasetType.CLOSED, List.of(), List.of());
        datasetService.save(dto);
        return dto;
    }
}