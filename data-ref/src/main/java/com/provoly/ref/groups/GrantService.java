package com.provoly.ref.groups;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.DashboardRepository;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.user.ProvolyUser;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GrantService {

    private Logger log;
    private DashboardRepository dashboardRepository;
    private GroupRepository groupRepository;
    private DatasetRepository datasetRepository;

    public GrantService(Logger log, DashboardRepository dashboardRepository,
            GroupRepository groupRepository,
            DatasetRepository datasetRepository) {
        this.log = log;
        this.dashboardRepository = dashboardRepository;
        this.groupRepository = groupRepository;
        this.datasetRepository = datasetRepository;
    }

    public void canWrite(EntityNamed entityNamed, WithGroupEntityType type, ProvolyUser user) {
        var canWrite = switch (type) {
            case DASHBOARD -> canWrite((Dashboard) entityNamed, user);
            case DATASET -> canWrite((Dataset) entityNamed, user);
        };

        if (!canWrite) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User is not granted to write %s %s.".formatted(type, entityNamed.getId()));
        }
    }

    private boolean canWrite(Dashboard dashboard, ProvolyUser user) {
        if (user.isAdmin() || user.equals(dashboard.getUser())) {
            return true;
        }

        return groupRepository.getGroupsByEntityId(dashboard.getId())
                .stream()
                .filter(g -> g.canWrite)
                .map(GroupRelations::getGroup)
                .anyMatch(g -> user.getGroups().contains(g));
    }

    private boolean canWrite(Dataset dataset, ProvolyUser user) {
        return user.isAdmin() || user.equals(dataset.getUser());
    }

    public boolean canSee(EntityNamed entityNamed, WithGroupEntityType type, ProvolyUser user) {
        log.debugf("Validating if user %s can see %s: %s", user.getId(), type, entityNamed.getId());
        boolean isUserOwner = switch (type) {
            case DASHBOARD -> (((Dashboard) entityNamed).getUser().equals(user));
            case DATASET -> (((Dataset) entityNamed).getUser().equals(user));
        };

        if (user.isAdmin() || isUserOwner) {
            return true;
        }

        var entityGrantedGroups = groupRepository.getEntityGroups(type, entityNamed);

        return isEntityNotPrivate(entityGrantedGroups)
                && entityGrantedGroups.stream().anyMatch(groupRelation -> user.getGroups().contains(groupRelation.group));
    }

    @Transactional
    public <T> List<T> getAllUserAllowed(WithGroupEntityType type, ProvolyUser user) {
        return (List<T>) switch (type) {
            case DASHBOARD -> getUserAllowedDashboards(user);
            case DATASET -> getUserAllowedDatasets(user);
        };
    }

    public Collection<Dataset> getUserAllowedDatasetsByClass(ProvolyUser user, UUID oclassId) {
        if (user.isAdmin()) {
            log.info("Admin user, getting all datasets");
            return datasetRepository.getAllForClass(oclassId);
        }
        log.infof("Get datasets for user %s with groups %s", user.getId(),
                user.getGroups().stream().map(Group::getName).toList());
        return datasetRepository.getClassDatasetsForUser(user, oclassId);
    }

    private Collection<Dashboard> getUserAllowedDashboards(ProvolyUser user) {
        if (user.isAdmin()) {
            log.info("Admin user, getting all dashboards");
            return dashboardRepository.getAll();
        }

        log.infof("Get dashboards for user %s with groups %s", user.getId(), user.getGroups());
        return dashboardRepository.getUserVisibleDashboards(user);
    }

    private Collection<Dataset> getUserAllowedDatasets(ProvolyUser user) {
        if (user.isAdmin()) {
            log.info("Admin user, getting all datasets");
            return datasetRepository.getAll();
        }
        return datasetRepository.getAllowedDatasetForUser(user);
    }

    private boolean isEntityNotPrivate(List<GroupRelations> groupsByEntity) {
        return groupsByEntity != null;
    }
}
