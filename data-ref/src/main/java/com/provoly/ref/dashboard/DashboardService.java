package com.provoly.ref.dashboard;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.SystemGroup;
import com.provoly.common.user.UserDto;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.datasource.DataSourceService;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.entity.GrantService;
import com.provoly.ref.groups.*;
import com.provoly.ref.message.notification.NotificationService;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.jboss.logging.Logger;

@ApplicationScoped
@Transactional
public class DashboardService {

    private UserService userService;

    private DashboardMapper dashboardMapper;

    private NotificationService notificationService;

    private EntityIdService entityIdService;

    private DataSourceService dataSourceService;
    private MetadataService metadataService;
    private GroupService groupService;

    private GrantService grantService;
    private Logger log;
    private DatasetService datasetService;

    private EntityManager em;

    public DashboardService(UserService userService, DashboardMapper dashboardMapper, NotificationService notificationService,
            EntityIdService entityIdService, DataSourceService dataSourceService, MetadataService metadataService,
            GroupService groupService, GrantService grantService, Logger log, DatasetService datasetService, EntityManager em) {
        this.userService = userService;
        this.dashboardMapper = dashboardMapper;
        this.notificationService = notificationService;
        this.entityIdService = entityIdService;
        this.dataSourceService = dataSourceService;
        this.metadataService = metadataService;
        this.groupService = groupService;
        this.grantService = grantService;
        this.log = log;
        this.datasetService = datasetService;
        this.em = em;
    }

    @Transactional
    public GroupErrors saveOrUpdate(DashboardWriteDto dashboardDto) {
        var dashboard = findById(dashboardDto.getId());

        if (dashboardDto.getDatasource() != null) {
            dataSourceService.allDataSourcesExist(dashboardDto.getDatasource());
        }

        if (dashboard == null) {
            ProvolyUser currentUser = userService.getCurrentUser();
            saveDashboard(dashboardDto, currentUser);
            return getGroupsError(getDashboard(dashboardDto.getId()), dashboardDto.getGroups());
        }
        return updateDashboard(dashboardDto, dashboard);
    }

    private GroupErrors updateDashboard(DashboardWriteDto dashboardDto, Dashboard dashboard) {
        log.infof("Update dashboard %s", dashboardDto.getId());
        // only creator can update dashboard
        grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD);
        dashboardMapper.update(dashboardDto, dashboard);
        metadataService.updateMetadataByEntityType(dashboardDto, EntityType.DASHBOARD);
        notificationService.sendNotification(dashboardDto, true);
        groupService.updateEntityGroups(dashboardDto.getGroups(), dashboardDto.getId(),
                WithGroupEntityType.DASHBOARD);
        return getGroupsError(dashboard, dashboardDto.getGroups());
    }

    private GroupErrors getGroupsError(Dashboard dashboard, List<String> groupsName) {
        var datasourceIds = dashboard.getDatasource();
        List<Group> groups = groupService.getGroupByNames(groupsName);
        Collection<UUID> datasetIds = datasetService.getAllFilterByDatasource(datasourceIds);
        Map<UUID, Set<String>> groupsErrors = new HashMap<>();

        datasetIds.forEach(datasetId -> {
            List<Group> datasetGroups = groupService.getGroupsByEntityId(datasetId).stream().toList();
            getDatasetMissingGroups(datasetGroups, groups)
                    .forEach(group -> groupsErrors.computeIfAbsent(datasetId, ignored -> new HashSet<>()).add(group));
        });
        return new GroupErrors(groupsErrors);
    }

    private Set<String> getDatasetMissingGroups(List<Group> datasetGroupIds, List<Group> dashboardGroupIds) {
        Set<String> missingGroups = new HashSet<>();

        dashboardGroupIds.forEach(group -> {
            if (isGroupAllRequiredForDataset(group.getId(), datasetGroupIds)) {
                missingGroups.add(SystemGroup.ALL.name());
            } else if (isGroupAuthenticatedRequiredForDataset(group.getId(), datasetGroupIds)) {
                missingGroups.add(SystemGroup.AUTHENTICATED.name());
            } else if (isGroupRequiredForDataset(group.getId(), datasetGroupIds)) {
                missingGroups.add(group.getName());
            }
        });
        return missingGroups;
    }

    private boolean isGroupAllRequiredForDataset(UUID dashboardGroupId, List<Group> datasetGroups) {
        return SystemGroup.ALL.is().test(dashboardGroupId)
                && datasetGroups.stream().map(Group::getId).noneMatch(groupId -> SystemGroup.ALL.is().test(groupId));
    }

    private boolean isGroupAuthenticatedRequiredForDataset(UUID dashboardGroupId, List<Group> datasetGroups) {
        return SystemGroup.AUTHENTICATED.is().test(dashboardGroupId)
                && datasetGroups.stream()
                        .map(Group::getId)
                        .noneMatch(groupId -> SystemGroup.AUTHENTICATED.is().or(SystemGroup.ALL.is()).test(groupId));
    }

    private boolean isGroupRequiredForDataset(UUID dashboardGroupId, List<Group> datasetGroups) {
        return datasetGroups.stream()
                .map(Group::getId)
                .noneMatch(groupId -> SystemGroup.AUTHENTICATED.is()
                        .or(SystemGroup.ALL.is())
                        .or(id -> id.equals(dashboardGroupId))
                        .test(groupId));
    }

    private void saveDashboard(DashboardWriteDto dashboardDto, ProvolyUser currentUser) {
        log.infof("Save dashboard %s", dashboardDto.getId());
        Dashboard dashboard = dashboardMapper.toModel(dashboardDto, currentUser);

        entityIdService.saveEntity(dashboard);
        if (dashboardDto.getMetadata() != null) {
            dashboardDto.getMetadata()
                    .forEach(metadata -> metadataService.addMetadataToEntity(dashboardDto.getId(),
                            metadata.getMetadataDefId(), metadata, EntityType.DASHBOARD));
        }
        if (dashboardDto.getGroups() == null) {
            log.debugf("No group provided for %s entity %s", EntityType.DASHBOARD, dashboardDto.getId());
            return;
        }
        log.debugf("Associate groups %s to %s %s", dashboardDto.getGroups(), WithGroupEntityType.DASHBOARD,
                dashboard.getId());
        groupService.associateGroupToEntity(dashboardDto.getGroups(), dashboardDto.getId(),
                WithGroupEntityType.DASHBOARD);
    }

    @Transactional
    public List<Dashboard> getCurrentUserAllowedDashboards() {
        UserDto currentUserDto = userService.getCurrentUserDto();
        log.infof("Get dashboards for user %s with groups %s", currentUserDto.getId(), currentUserDto.getGroups());
        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT dashboard.id FROM dashboard " +
                        "LEFT JOIN group_relations as gr ON dashboard.id = gr.entity_id " +
                        "LEFT JOIN group_def as gd ON gr.group_id = gd.id " +
                        "WHERE gd.name in :groups_names OR dashboard.user_id = :user_id ) "
                        + "SELECT * FROM dashboard where id in (SELECT id FROM ids)",
                Dashboard.class)
                .setParameter("user_id", currentUserDto.getId())
                .setParameter("groups_names", currentUserDto.getGroups())
                .getResultList();
    }

    @Transactional
    public void delete(UUID id) {
        var dashboard = getDashboard(id);
        grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD);
        entityIdService.removeEntity(id, Dashboard.class);

        notificationService.sendNotification(dashboardMapper.toReadDto(dashboard), false);
    }

    @Transactional
    public Dashboard getCurrentUserAllowedDashboardById(UUID dashboardId) {
        UserDto currentUserDto = userService.getCurrentUserDto();
        var query = em.createNativeQuery(
                "WITH dashboardId AS (SELECT DISTINCT dashboard.id FROM dashboard " +
                        "LEFT JOIN group_relations as gr ON dashboard.id = gr.entity_id " +
                        "LEFT JOIN group_def as gd ON gr.group_id = gd.id " +
                        "WHERE gd.name in :groups_names OR dashboard.user_id = :user_id) " +
                        "SELECT * FROM dashboard where id = :dashboard_id AND :dashboard_id IN (SELECT id FROM dashboardId)",
                Dashboard.class)
                .setParameter("user_id", currentUserDto.getId())
                .setParameter("groups_names", currentUserDto.getGroups())
                .setParameter("dashboard_id", dashboardId);
        try {
            return (Dashboard) query.getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is not granted to get dashboard %s.".formatted(dashboardId));
        }
    }

    @Transactional
    public Dashboard findById(UUID dashboardId) {
        return entityIdService.findById(dashboardId, Dashboard.class);
    }

    @Transactional
    public Dashboard getDashboard(UUID id) {
        return entityIdService.getById(id, Dashboard.class);
    }

    @Transactional
    public Collection<Dashboard> getAll() {
        return entityIdService.getAll(Dashboard.class);
    }
}