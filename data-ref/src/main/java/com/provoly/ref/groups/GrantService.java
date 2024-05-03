package com.provoly.ref.groups;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Path;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.DashboardRepository;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GrantService {

    private UserService userService;
    private EntityManager em;
    private Logger log;
    private DashboardRepository dashboardRepository;
    private GroupRepository groupRepository;
    private DatasetRepository datasetRepository;

    public GrantService(UserService userService, EntityManager em, Logger log, DashboardRepository dashboardRepository,
            GroupRepository groupRepository,
            DatasetRepository datasetRepository) {
        this.userService = userService;
        this.em = em;
        this.log = log;
        this.dashboardRepository = dashboardRepository;
        this.groupRepository = groupRepository;
        this.datasetRepository = datasetRepository;
    }

    public void canWrite(EntityNamed entityNamed, WithGroupEntityType type) {
        var user = userService.getCurrentUser();
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
        boolean isUserOwner = switch (type) {
            case DASHBOARD -> (((Dashboard) entityNamed).getUser().equals(userService.getCurrentUser()));
            case DATASET -> (((Dataset) entityNamed).getUser().equals(userService.getCurrentUser()));
        };

        if (isUserOwner) {
            return true;
        }

        HashSet<UUID> userGroupsId = getUserGroupsId(user);
        var entityGrantedGroups = groupRepository.getEntityGroups(type, entityNamed);

        return isEntityNotPrivate(entityGrantedGroups)
                && entityGrantedGroups.stream().anyMatch(groupRelation -> userGroupsId.contains(groupRelation.group.getId()));
    }

    @Transactional
    public <T> List<T> getAllUserAllowed(WithGroupEntityType type, ProvolyUser user) {

        return (List<T>) switch (type) {
            case DASHBOARD -> getUserAllowedDashboards(user);
            case DATASET -> getUserAllowedDatasets(user);
        };
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
            datasetRepository.getAll();
        }
        return datasetRepository.getAllowedDataset(user);
    }

    private boolean isEntityNotPrivate(List<GroupRelations> groupsByEntity) {
        return groupsByEntity != null;
    }

    private HashSet<UUID> getUserGroupsId(ProvolyUser user) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(Group.class);
        Path<UUID> idPath = root.get(Group_.ID);
        q = q.select(idPath);
        q = q.where(
                cb.in(root.get(Group_.NAME)).value(user.getGroups().stream().map(Group::getName).toList()));
        return em.createQuery(q).getResultStream().collect(Collectors.toCollection(HashSet::new));
    }
}
