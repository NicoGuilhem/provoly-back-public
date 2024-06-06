package com.provoly.ref.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.groups.*;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DashboardServiceTest {
    @Inject
    TestService testService;

    @Inject
    DashboardService dashboardService;
    @Inject
    DashboardRepository dashboardRepository;

    @Inject
    GroupService groupService;
    @Inject
    GroupRepository groupRepository;

    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    DatasetService datasetService;
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    EntityManager entityManager;

    @AfterEach
    @Transactional
    public void deleteDashboards() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);

        dashboardRepository.getAll().forEach(d -> {
            var groups = groupRepository.getGroupsByEntityId(d.getId());
            groups.forEach(group -> {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaDelete<GroupRelations> delete = cb.createCriteriaDelete(GroupRelations.class);
                Root<GroupRelations> e = delete.from(GroupRelations.class);
                delete.where(cb.equal(e.get("entityId"), group.getId()));
                entityManager.createQuery(delete).executeUpdate();
            });
            dashboardService.delete(d.getId());
        });
    }

    private void createAsAdminPublicDashboardAndDashboardWithGroups(String... groups) {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);

        dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "authenticated", null, null, false,
                List.of(), Map.of(), null,
                Arrays.stream(groups).collect(Collectors.toMap(g -> g, g -> List.of(GroupRights.READ)))));

        dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "all", null, null, false,
                List.of(), Map.of(), null, Map.of("ALL", List.of(GroupRights.READ))));
    }

    @Test
    public void should_get_all_and_authenticated_dashboards_as_authenticated_user() {
        // given
        createAsAdminPublicDashboardAndDashboardWithGroups("AUTHENTICATED");

        //when
        var res = dashboardService.getCurrentUserAllowedDashboards();

        //then
        assertThat(res)
                .extracting(Dashboard::getName)
                .containsExactlyInAnyOrder("all", "authenticated");
    }

    @Test
    public void should_save_dashboard_group_admin_when_authenticated_as_admin() {
        // given
        testService.ensureGroups(List.of("admin"));
        createAsAdminPublicDashboardAndDashboardWithGroups("AUTHENTICATED", "admin");

        //when
        var res = dashboardService.getCurrentUserAllowedDashboards();

        //then
        assertThat(res)
                .extracting(Dashboard::getName)
                .containsExactlyInAnyOrder("all", "authenticated");
    }

    @Test
    public void should_not_save_dashboard_with_unknown_group() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        //then
        assertThatThrownBy(
                () -> dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "AUTHENTICATED", null, null, false,
                        List.of(), Map.of(), null, Map.of("unknown", List.of(GroupRights.READ)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Group unknown doesn't exist.");
    }

    @Test
    public void should_not_access_private_dashboard_as_other_user() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "authenticated", null, null, false,
                List.of(), Map.of(), null, Map.of()));

        //when
        testService.authenticate("iampolice", currentSubjectProvider);
        var res = dashboardService.getCurrentUserAllowedDashboards();

        //then
        assertThat(res).isEmpty();

    }

    @Test
    public void should_not_modify_private_dashboard_as_other_user() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        UUID dashboardId = UUID.randomUUID();
        dashboardService.saveOrUpdate(new DashboardWriteDto(dashboardId, "authenticated", null, null, false,
                List.of(), Map.of(), null, Map.of()));

        //when
        testService.authenticate("iampolice", currentSubjectProvider);

        //then
        assertThatThrownBy(
                () -> dashboardService.saveOrUpdate(new DashboardWriteDto(dashboardId, "authenticated", null, null, false,
                        List.of(), Map.of(), null, Map.of("ALL", List.of(GroupRights.READ)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "User is not granted to write %s %s.".formatted(WithGroupEntityType.DASHBOARD, dashboardId));

    }

    @Test
    public void should_return_missing_group_AUTHENTICATED_in_dashboard_when_save() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        groupService.addGroup(new GroupWrite(UUID.randomUUID(), "nouveau_group"));
        DatasetDto datasetDto = initDataset("nouveau_group");

        //when
        var result = dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "authenticated", null, null, false,
                List.of(datasetDto.getId()), Map.of(), null, Map.of("AUTHENTICATED", List.of(GroupRights.READ))));

        //then
        Map<UUID, Set<String>> missingGroup = new HashMap<>();
        missingGroup.put(datasetDto.getId(), Set.of("AUTHENTICATED"));
        assertThat(result)
                .extracting(GroupErrors::missingGroupsByEntity)
                .isEqualTo(missingGroup);
    }

    @Test
    public void should_return_missing_group_ALL_in_dashboard_when_save() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        groupService.addGroup(new GroupWrite(UUID.randomUUID(), "nouveau_group_A"));
        DatasetDto datasetDto = initDataset("nouveau_group_A");

        //when
        var result = dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "all", null, null, false,
                List.of(datasetDto.getId()), Map.of(), null, Map.of("ALL", List.of(GroupRights.READ))));

        //then
        Map<UUID, Set<String>> missingGroup = new HashMap<>();
        missingGroup.put(datasetDto.getId(), Set.of("ALL"));
        assertThat(result)
                .extracting(GroupErrors::missingGroupsByEntity)
                .isEqualTo(missingGroup);
    }

    @Test
    public void should_return_missing_group_ALL_in_dashboard_when_add_new_datasource_in_update() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.ensureGroups(List.of("nouveau_group_B"));
        DatasetDto datasetDto = initDataset("nouveau_group_B");
        DatasetDto datasetDto2 = initDataset("nouveau_group_B");
        UUID dashboardId = UUID.randomUUID();

        //when
        dashboardService.saveOrUpdate(new DashboardWriteDto(dashboardId, "all", null, null, false,
                List.of(datasetDto.getId()), Map.of(), null, Map.of("ALL", List.of(GroupRights.READ))));
        var result = dashboardService.saveOrUpdate(new DashboardWriteDto(dashboardId, "all", null, null, false,
                List.of(datasetDto.getId(), datasetDto2.getId()), Map.of(), null, Map.of("ALL", List.of(GroupRights.READ))));

        //then
        Map<UUID, Set<String>> missingGroup = new HashMap<>();
        missingGroup.put(datasetDto.getId(), Set.of("ALL"));
        missingGroup.put(datasetDto2.getId(), Set.of("ALL"));
        assertThat(result)
                .extracting(GroupErrors::missingGroupsByEntity)
                .isEqualTo(missingGroup);
    }

    @Test
    public void should_return_missing_group_in_dashboard_when_save() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.ensureGroups(List.of("no_group", "test", "un_group"));
        DatasetDto datasetDto = initDataset("un_group");
        DatasetDto datasetDto2 = initDataset("test");

        //when
        var result = dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "authenticated", null, null, false,
                List.of(datasetDto.getId(), datasetDto2.getId()), Map.of(), null,
                Map.of("no_group", List.of(GroupRights.READ))));

        //then
        Map<UUID, Set<String>> missingGroup = new HashMap<>();
        missingGroup.put(datasetDto.getId(), Set.of("no_group"));
        missingGroup.put(datasetDto2.getId(), Set.of("no_group"));
        assertThat(result)
                .extracting(GroupErrors::missingGroupsByEntity)
                .isEqualTo(missingGroup);
    }

    @Test
    public void should_not_return_any_missing_group_in_dashboard_when_save() {
        // given
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        testService.ensureGroups(List.of("test", "un_group"));
        DatasetDto datasetDto = initDataset("un_group", "test");
        DatasetDto datasetDto2 = initDataset("test", "un_group");

        //when
        var result = dashboardService.saveOrUpdate(new DashboardWriteDto(UUID.randomUUID(), "authenticated", null, null, false,
                List.of(datasetDto.getId(), datasetDto2.getId()), Map.of(), null,
                Map.of("un_group", List.of(GroupRights.READ), "test", List.of(GroupRights.READ))));

        //then
        assertThat(result)
                .extracting(GroupErrors::missingGroupsByEntity)
                .isEqualTo(new HashMap<>());
    }

    private DatasetDto initDataset(String... groups) {
        UUID fieldId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        testService.createAndSaveField(fieldId);
        AttributeDefDto attributeDefDto = testService.createAttributeDto(attributeId, "attributeName",
                "attributeId" + attributeId, fieldId);
        OClassWriteDto classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));

        DatasetDto dto = new DatasetDto(UUID.randomUUID(), "test", classDto.getId(), DatasetType.CLOSED, List.of(groups),
                List.of());
        datasetService.save(dto);
        return dto;
    }
}