package com.provoly.ref.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.*;
import com.provoly.common.user.Role;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetMapper;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.utils.TestService;
import com.provoly.ref.widget.WidgetCatalog;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NamedQueryControllerTest {

    @Inject
    NamedQueryController namedQueryController;

    @Inject
    NamedQueryService namedQueryService;

    @Inject
    ModelService modelService;

    @Inject
    ModelMapper modelMapper;

    @Inject
    TestService testService;

    @Inject
    DatasetRepository datasetRepository;

    @Inject
    DatasetMapper datasetMapper;

    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    UserService userService;

    private OClassWriteDto classDtoTest;
    private final UUID nqPublicId = UUID.randomUUID();
    private final UUID nqPrivateId = UUID.randomUUID();
    private final UUID widgetId = UUID.fromString("4bec840c-c12b-4ced-8b4f-5acef7cedee6");

    private NamedQueryDto createNamedQueryDto(UUID id, String name, VisibilityType visibilityType) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name(), List.of());
        return new NamedQueryDto(id, name, "description_nq", new MonoClassRequestDto(classDtoTest.getId(), null), vis);
    }

    private NamedQueryDto createNamedQueryDto(UUID id, String name, VisibilityType visibilityType,
            List<UUID> datasetVersionIds) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name());
        return new NamedQueryDto(id, name, "description_nq", new MonoClassRequestDto(classDtoTest.getId(), datasetVersionIds),
                vis);
    }

    private NamedQueryDto createMultiNamedQueryDto(UUID id) {
        VisibilityDto vis = new VisibilityDto(VisibilityType.PRIVATE.name());
        return new NamedQueryDto(id, "multi_nq", "description_nq",
                new MultiClassRequestDto(MultiSearchType.AND, List.of(classDtoTest.getId()), null), vis);
    }

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        // add class
        classDtoTest = new OClassWriteDto(UUID.fromString("999a19f5-9d00-4028-b4b6-4b04101f6316"), "classTest",
                new ArrayList<>(),
                Storage.ELASTIC);
        if (!modelService.exists(modelMapper.toModel(classDtoTest))) {
            modelService.saveEntity(modelMapper.toModel(classDtoTest));
        }
        //iamsuperadmin create private and public namedquery
        namedQueryController.saveNamedQuery(createNamedQueryDto(nqPrivateId, "namedqueryPrivate", VisibilityType.PRIVATE));
        namedQueryController.saveNamedQuery(createNamedQueryDto(nqPublicId, "namedqueryPublic", VisibilityType.PUBLIC));
    }

    @AfterEach
    @Transactional
    public void clear() {
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void police_should_not_get_private_named_query_created_by_admin() {
        testService.authenticate("iampolice", currentSubjectProvider);
        var nqs = namedQueryController.getMines();
        assertThat(nqs).hasSize(1);
        assertThat(nqs.stream().toList().getFirst().isOwner()).isFalse();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void admin_should_get_all_named_query_created_by_admin() {
        assertThat(namedQueryController.getMines()).hasSize(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void police_should_not_set_favorite_private_namedquery() {
        testService.authenticate("iampolice", currentSubjectProvider);
        assertThatThrownBy(() -> namedQueryController.addToMineFavorite(nqPrivateId)).isInstanceOf(BusinessException.class);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void police_should_set_favorite_public_namedquery() {
        testService.authenticate("iampolice", currentSubjectProvider);
        namedQueryController.getUserNamedQueryById(nqPublicId);
        namedQueryController.addToMineFavorite(nqPublicId);

        var result = namedQueryService.getMineById(nqPublicId);

        assertThat(result).extracting(nq -> nq.getNamedQuery().getId()).isEqualTo(nqPublicId);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void police_should_not_update_public_namedquery() {
        testService.authenticate("iampolice", currentSubjectProvider);
        var dto = createNamedQueryDto(nqPublicId, "POLICEnamedqueryPublic", VisibilityType.PUBLIC, List.of());
        Assertions.assertThatThrownBy(() -> namedQueryController.saveNamedQuery(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void admin_should_not_delete_public_namedquery() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        Assertions.assertThatThrownBy(() -> namedQueryController.deleteForMe(nqPublicId))
                .isInstanceOf(BusinessException.class).hasMessageContaining("PUBLIC");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void admin_should_not_delete_private_used_named_query() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        WidgetCatalog wc = new WidgetCatalog(widgetId);
        wc.setName("widget");
        wc.setContent("");
        wc.setVisibilityType(VisibilityType.PRIVATE);
        wc.setDatasource(List.of(nqPrivateId));
        wc.setCreationDate(Instant.now());
        wc.setModificationDate(Instant.now());
        modelService.saveEntity(wc);

        Assertions.assertThatThrownBy(() -> namedQueryController.deleteForMe(nqPrivateId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void admin_should_get_multiclass_named_query() {
        namedQueryService.removeNamedQueryIfExists(nqPrivateId);
        namedQueryService.removeNamedQueryIfExists(nqPublicId);
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        var nq = createMultiNamedQueryDto(nqPrivateId);

        namedQueryService.saveNamedQueryForUser(nq);

        var retreived_nq = namedQueryController.getUserNamedQueryById(nqPrivateId);

        var request = (MultiClassRequestDto) retreived_nq.getRequest();
        assertThat(request.getoClasses()).containsExactly(classDtoTest.getId());

    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void should_shared_namedqueries_public() {
        // GIVEN
        testService.authenticate("iampolice", currentSubjectProvider);

        // WHEN
        var namedQueryDetailsDtos = namedQueryController.getMines();

        // THEN
        assertThat(namedQueryDetailsDtos).hasSize(1);
        assertThat(namedQueryDetailsDtos.stream().findFirst().get().getVisibility().getType())
                .isEqualTo(VisibilityType.PUBLIC.toString());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void should_shared_namedqueries_mines_public() {
        // GIVEN
        testService.authenticate("iamsuperadmin", currentSubjectProvider);

        // WHEN
        var namedQueryDetailsDtos = namedQueryController.getMines();

        // THEN
        assertThat(namedQueryDetailsDtos).hasSize(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void delete_namequery_and_associated_entities() {
        // GIVEN
        testService.authenticate("iamsuperadmin", currentSubjectProvider);

        // WHEN
        namedQueryController.deleteForMe(nqPrivateId);

        // THEN
        assertThat(namedQueryService.findById(nqPrivateId)).isNull();
        assertThat(namedQueryService.getNamedQueriesForCurrentUser().stream()
                .filter(nq -> nq.getNamedQuery().getId().equals(nqPrivateId))
                .findFirst())
                .isEmpty();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void createNamedqueryLinkToDatasetWithoutDatasetVersion_shouldThrowError400() {
        // GIVEN
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID attributeId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        testService.createAndSaveField(fieldId);
        AttributeDefDto attributeDefDto = testService.createAttributeDto(attributeId, "attributeName",
                "attributeId" + attributeId, fieldId);
        OClassWriteDto classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));
        DatasetDto datasetDto = createDatasetDto(classDto);

        NamedQueryDto nq = createNamedQueryDto(nqPublicId, "POLICEnamedqueryPublic", VisibilityType.PUBLIC,
                List.of(datasetDto.getId()));
        assertThatThrownBy(() -> namedQueryService.saveNamedQueryForUser(nq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "DatasetVersion : %s inexistant.".formatted(datasetDto.getId()));
    }

    private DatasetDto createDatasetDto(OClassWriteDto classDto) {
        ProvolyUser provolyUser = userService.getCurrentUser();
        DatasetDto datasetDto = new DatasetDto(
                UUID.fromString("999aa06c-9204-4cc5-849b-7d348316bec6"),
                "c@r@ctér`st`qu€",
                classDto.getId(),
                DatasetType.CLOSED);
        Dataset dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetRepository.save(dataset);
        return datasetDto;
    }
}