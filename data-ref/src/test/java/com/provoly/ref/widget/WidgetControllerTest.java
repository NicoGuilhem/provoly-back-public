package com.provoly.ref.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.common.search.NamedQueryDto;
import com.provoly.common.search.VisibilityDto;
import com.provoly.common.user.Role;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetMapper;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.NamedQueryService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.utils.TestService;
import com.provoly.ref.widget.dto.WidgetDto;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WidgetControllerTest {
    @Inject
    WidgetController widgetController;
    @Inject
    WidgetService widgetService;
    @Inject
    DatasetVersionMapper datasetVersionMapper;
    @Inject
    NamedQueryService namedQueryService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    ModelService modelService;
    @Inject
    DatasetVersionService datasetVersionService;
    @Inject
    DatasetService datasetService;
    @Inject
    DatasetMapper datasetMapper;
    @Inject
    TestService testService;
    @Inject
    UserService userService;

    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    UUID privateId = UUID.fromString("d64155fc-9e36-4fc3-84f3-19f2e12ea851");
    UUID publicId = UUID.fromString("d13d7cf3-4b86-458b-8113-f04c613e584d");
    OClassWriteDto classDtoTest = new OClassWriteDto(UUID.fromString("999a19f5-9d00-4028-b4b6-4b04101f6316"), "classTest",
            new ArrayList<>(), Storage.ELASTIC);

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.clean();
        OClass oClass = modelMapper.toModel(classDtoTest);
        if (!modelService.exists(oClass)) {
            modelService.saveEntity(oClass);
        }
        createWidget("public", "dsd public test name", VisibilityType.PUBLIC);
        createWidget("private", "dsd private test name", VisibilityType.PRIVATE);

    }

    @AfterEach
    @Transactional
    public void cleanDatasetVersionsAndDatasets() {
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_WRITE, Role.STR_WIDGET_CATALOG_READ })
    public void should_add_widget_in_catalog() {
        WidgetDto dto = createWidget("mywidget", "should_add_widget_in_catalog DSname", VisibilityType.PUBLIC);
        widgetController.addWidget(dto);
        var saved = widgetController.getWidget(dto.id);
        assertThat(saved).isNotNull();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_WRITE })
    public void should_not_add_widget_json_invalid() {
        WidgetDto dto = createWidget("mywidget", "should_not_add_widget_json_invalid DSname", VisibilityType.PUBLIC);
        dto.content = "{";
        assertThatThrownBy(() -> widgetController.addWidget(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_WRITE })
    public void should_not_add_widget_name_already_used() {
        WidgetDto dto = createWidget("widget2", "should_not_add_widget_name_already_used DSname", VisibilityType.PUBLIC);
        widgetController.addWidget(dto);
        dto.id = UUID.randomUUID();

        assertThatThrownBy(() -> widgetController.addWidget(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_WRITE, Role.STR_WIDGET_CATALOG_READ })
    public void should_not_get_widget_not_exists() {
        UUID widgetId = UUID.randomUUID();
        assertThatThrownBy(() -> widgetController.getWidget(widgetId))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("inexistant");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_READ, Role.STR_WIDGET_CATALOG_WRITE })
    public void should_not_get_widget_forbidden() {
        testService.authenticate("iampolice", currentSubjectProvider);
        assertThatThrownBy(() -> widgetController.getWidget(privateId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("doesn't belong to user");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_READ, Role.STR_WIDGET_CATALOG_WRITE })
    public void should_get_widget_public() {
        testService.authenticate("iampolice", currentSubjectProvider);
        var res = widgetController.getWidget(publicId);
        assertThat(res).isNotNull();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_READ, Role.STR_WIDGET_CATALOG_WRITE })
    public void should_get_widget_public_with_cover_default_false() {
        testService.authenticate("iampolice", currentSubjectProvider);
        var res = widgetController.getWidget(publicId);
        assertThat(res).isNotNull();
        assertThat(res.cover).isFalse();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_READ, Role.STR_WIDGET_CATALOG_WRITE })
    public void should_update_widget_catalog_modification_date_on_update() {
        // Given
        Instant previousModificationDate = widgetService.getMineById(publicId).getWidgetCatalog()
                .getModificationDate();
        WidgetDto dto = widgetController.getWidget(publicId);
        dto.description = "nouvelle description";

        // When
        widgetController.addWidget(dto);
        var result = widgetController.getWidget(publicId);

        // Then
        BDDAssertions.then(result.modificationDate).isNotNull();
        BDDAssertions.then(result.modificationDate).isNotEqualTo(previousModificationDate);
        BDDAssertions.then(result.description).isEqualTo("nouvelle description");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_WIDGET_CATALOG_READ, Role.STR_WIDGET_CATALOG_WRITE })
    public void should_get_widget_public_with_creation_and_modification_date_default_values() {
        // When
        var result = widgetController.getWidget(publicId);

        // Then
        BDDAssertions.then(result).isNotNull();
        BDDAssertions.then(result.creationDate).isNotNull();
        BDDAssertions.then(result.modificationDate).isNotNull();
    }

    private WidgetDto createWidget(String widgetName, String datasetTestName, VisibilityType visibilityType) {
        UUID namedQueryId = UUID.randomUUID();
        UUID datasetVersionId = UUID.randomUUID();
        UUID datasetId = UUID.randomUUID();
        creatAndSaveDatasources(namedQueryId, datasetVersionId, datasetId, datasetTestName);

        WidgetDto widget = new WidgetDto();
        if (visibilityType == VisibilityType.PRIVATE) {
            widget.id = privateId;
            widget.visibility = new VisibilityDto(VisibilityType.PRIVATE.name(), List.of());
        } else {
            widget.id = publicId;
            widget.visibility = new VisibilityDto(VisibilityType.PUBLIC.name(), List.of());
        }

        widget.name = widgetName;
        widget.content = "";
        widget.datasource = List.of(namedQueryId, datasetVersionId, datasetId);
        widgetController.addWidget(widget);
        return widget;
    }

    private void creatAndSaveDatasources(UUID namedQueryId, UUID datasetVersionId, UUID datasetId,
            String datasetTestName) {
        createNamedQuery(namedQueryId);
        createDatasetVersionAndDataset(datasetVersionId, datasetId, datasetTestName);
    }

    private void createNamedQuery(UUID id) {
        VisibilityDto visibilityDto = new VisibilityDto(VisibilityType.PUBLIC.toString(), List.of());
        OClassWriteDto classDtoTest = new OClassWriteDto(UUID.fromString("999a19f5-9d00-4028-b4b6-4b04101f6316"),
                "classTest",
                new ArrayList<>(), Storage.ELASTIC);
        if (!modelService.exists(modelMapper.toModel(classDtoTest))) {
            modelService.saveEntity(modelMapper.toModel(classDtoTest));
        }
        NamedQueryDto namedQueryDto = new NamedQueryDto(id, "test name", "description",
                new MonoClassRequestDto(classDtoTest.getId(), List.of()), visibilityDto);
        namedQueryService.saveNamedQueryForUser(namedQueryDto);
    }

    private void createDatasetVersionAndDataset(UUID datasetVersionId, UUID datasetId, String datasetTestName) {
        ProvolyUser provolyUser = userService.getCurrentUser();
        DatasetDto datasetDto = new DatasetDto(datasetId, datasetTestName, classDtoTest.getId(),
                DatasetType.MODIFIABLE);
        DatasetVersionDto datasetVersionDto = new DatasetVersionDto(datasetVersionId, datasetId, DatasetState.ACTIVE);
        Dataset dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetService.saveEntity(dataset);
        DatasetVersion datasetVersion = datasetVersionMapper.toModel(datasetVersionDto);
        datasetVersionService.createDatasetVersion(datasetVersion);
    }
}