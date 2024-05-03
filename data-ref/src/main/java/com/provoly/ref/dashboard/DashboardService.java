package com.provoly.ref.dashboard;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.user.SystemGroup;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.datasource.DataSourceService;
import com.provoly.ref.entity.EntityType;
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
    private DataSourceService dataSourceService;
    private MetadataService metadataService;
    private GroupService groupService;
    private GroupRepository groupRepository;
    private GrantService grantService;
    private Logger log;
    private DatasetRepository datasetRepository;

    private DashboardRepository dashboardRepository;

    public DashboardService(UserService userService, DashboardMapper dashboardMapper, NotificationService notificationService,
            DataSourceService dataSourceService, MetadataService metadataService,
            GroupService groupService, GroupRepository groupRepository, GrantService grantService, Logger log,
            DatasetRepository datasetRepository,
            DashboardRepository dashboardRepository) {
        this.userService = userService;
        this.dashboardMapper = dashboardMapper;
        this.notificationService = notificationService;
        this.dataSourceService = dataSourceService;
        this.metadataService = metadataService;
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.grantService = grantService;
        this.log = log;
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional
    public GroupErrors saveOrUpdate(DashboardWriteDto dashboardDto) {
        var dashboard = dashboardRepository.findById(dashboardDto.getId());

        if (dashboardDto.getDatasource() != null) {
            dataSourceService.allDataSourcesExist(dashboardDto.getDatasource());
        }

        if (dashboard == null) {
            ProvolyUser currentUser = userService.getCurrentUser();
            saveDashboard(dashboardDto, currentUser);
            return getGroupsError(dashboardRepository.getDashboard(dashboardDto.getId()),
                    groupService.getGroupsNames(dashboardDto.getAccessRightsByGroup()));
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
        groupService.updateEntityGroups(dashboardDto.getAccessRightsByGroup(), dashboardDto.getId(),
                WithGroupEntityType.DASHBOARD);
        return getGroupsError(dashboard, groupService.getGroupsNames(dashboardDto.getAccessRightsByGroup()));
    }

    private GroupErrors getGroupsError(Dashboard dashboard, Collection<String> groupsName) {
        var datasourceIds = dashboard.getDatasource();
        List<Group> groups = groupRepository.getGroupByNames(groupsName);
        Collection<UUID> datasetIds = datasetRepository.getAllFilterByDatasource(datasourceIds);
        Map<UUID, Set<String>> groupsErrors = new HashMap<>();

        datasetIds.forEach(datasetId -> {
            List<Group> datasetGroups = groupRepository.getGroupsByEntityId(datasetId).stream().map(GroupRelations::getGroup)
                    .toList();
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

        dashboardRepository.save(dashboard);
        if (dashboardDto.getMetadata() != null) {
            dashboardDto.getMetadata()
                    .forEach(metadata -> metadataService.addMetadataToEntity(dashboardDto.getId(),
                            metadata.getMetadataDefId(), metadata, EntityType.DASHBOARD));
        }
        if (dashboardDto.getAccessRightsByGroup() == null) {
            log.debugf("No group provided for %s entity %s", EntityType.DASHBOARD, dashboardDto.getId());
            return;
        }
        log.debugf("Associate groups %s to %s %s", dashboardDto.getAccessRightsByGroup(), WithGroupEntityType.DASHBOARD,
                dashboard.getId());
        groupService.associateGroupToEntity(dashboardDto.getAccessRightsByGroup(), dashboardDto.getId(),
                WithGroupEntityType.DASHBOARD);
    }

    @Transactional
    public List<Dashboard> getCurrentUserAllowedDashboards() {
        ProvolyUser currentUser = userService.getCurrentUser();
        log.infof("Get dashboards for user %s with groups %s", currentUser.getId(), currentUser.getGroups());
        return grantService.getAllUserAllowed(WithGroupEntityType.DASHBOARD, currentUser);
    }

    @Transactional
    public void delete(UUID id) {
        var dashboard = dashboardRepository.getDashboard(id);
        grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD);
        dashboardRepository.delete(id);

        notificationService.sendNotification(dashboardMapper.toReadDto(dashboard), false);
    }

    @Transactional
    public Dashboard getDashboardById(UUID dashboardId) {
        ProvolyUser currentUser = userService.getCurrentUser();
        return Optional.of(dashboardRepository.getDashboard(dashboardId))

                .filter(dashboard -> grantService.canSee(dashboard, WithGroupEntityType.DASHBOARD, currentUser))
                .orElseThrow(() -> new ProvolyNotFoundException("Dashboard : %s inexistant.".formatted(dashboardId)));
    }
}