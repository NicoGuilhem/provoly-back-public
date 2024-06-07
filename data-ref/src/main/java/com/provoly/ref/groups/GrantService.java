package com.provoly.ref.groups;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.model.OClass_;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.widget.WidgetCatalog;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GrantService {

    private Logger log;
    private EntityIdService entityIdService;
    private GroupRepository groupRepository;
    private DatasetRepository datasetRepository;

    public GrantService(Logger log, EntityIdService entityIdService,
            GroupRepository groupRepository,
            DatasetRepository datasetRepository) {
        this.log = log;
        this.entityIdService = entityIdService;
        this.groupRepository = groupRepository;
        this.datasetRepository = datasetRepository;
    }

    public void canWrite(EntityNamed entityNamed, WithGroupEntityType type, ProvolyUser user) {
        if (user.isAdmin()) {
            return;
        }
        var canWrite = switch (type) {
            case DASHBOARD -> canWrite((Dashboard) entityNamed, user);
            case DATASET -> canWrite((Dataset) entityNamed, user);
            case WIDGET -> canWrite((WidgetCatalog) entityNamed, user);
        };

        if (!canWrite) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User is not granted to write %s %s.".formatted(type, entityNamed.getId()));
        }
    }

    private boolean canWrite(Dashboard dashboard, ProvolyUser user) {
        if (user.equals(dashboard.getUser())) {
            return true;
        }

        return groupRepository.getGroupsByEntityId(dashboard.getId())
                .stream()
                .filter(g -> g.canWrite)
                .map(GroupRelations::getGroup)
                .anyMatch(g -> user.getGroups().contains(g));
    }

    private boolean canWrite(WidgetCatalog widgetCatalog, ProvolyUser user) {
        return user.equals(widgetCatalog.getUser());
    }

    private boolean canWrite(Dataset dataset, ProvolyUser user) {
        return user.equals(dataset.getUser());
    }

    public boolean canSee(EntityNamed entityNamed, WithGroupEntityType type, ProvolyUser user) {
        log.debugf("Validating if user %s can see %s: %s", user.getId(), type, entityNamed.getId());
        boolean isUserOwner = switch (type) {
            case DASHBOARD -> (((Dashboard) entityNamed).getUser().equals(user));
            case DATASET -> (((Dataset) entityNamed).getUser().equals(user));
            case WIDGET -> (((WidgetCatalog) entityNamed).getUser().equals(user));
        };

        if (user.isAdmin() || isUserOwner) {
            return true;
        }

        var entityGrantedGroups = groupRepository.getEntityGroups(type, entityNamed);

        return isEntityNotPrivate(entityGrantedGroups)
                && entityGrantedGroups.stream().anyMatch(groupRelation -> user.getGroups().contains(groupRelation.group));
    }

    @Transactional
    public <T extends EntityId> List<T> getAllUserAllowed(WithGroupEntityType type, ProvolyUser user) {
        if (!user.isAdmin()) {
            log.infof("Get %s for user %s with groups %s", type.getEntity().getSimpleName(), user.getId(),
                    user.getGroups().stream().map(Group::getName).toList());
            return groupRepository.getAllowedEntityId(user, type.getEntity());
        }

        log.infof("Admin user, getting all %s", type.getEntity().getSimpleName());
        return entityIdService.getAll(type.getEntity());
    }

    //TODO refacto this method to make it generic for all EntityId (not specific to Datasets)
    public Collection<Dataset> getUserAllowedDatasetsByClass(final @NotNull ProvolyUser user, final UUID oclassId) {
        if (user.isAdmin()) {
            log.info("Admin user, getting all datasets");
            return datasetRepository.getAllForClass(oclassId);
        }
        log.infof("Get datasets for user %s with groups %s", user.getId(),
                user.getGroups().stream().map(Group::getName).toList());
        return groupRepository.getAllowedEntityId(user, Dataset.class,
                (cb, query, root) -> cb.equal(root.get(Dataset_.oClass).get(OClass_.id), oclassId));
    }

    private boolean isEntityNotPrivate(List<GroupRelations> groupsByEntity) {
        return groupsByEntity != null;
    }
}
