package com.provoly.ref.groups;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.model.OClass_;
import com.provoly.ref.user.ProvolyUser;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GrantService {

    private Logger log;
    private EntityIdRepository entityIdRepository;
    private GroupRepository groupRepository;
    private DatasetRepository datasetRepository;

    public GrantService(Logger log, EntityIdRepository entityIdRepository,
            GroupRepository groupRepository,
            DatasetRepository datasetRepository) {
        this.log = log;
        this.entityIdRepository = entityIdRepository;
        this.groupRepository = groupRepository;
        this.datasetRepository = datasetRepository;
    }

    private boolean isUserGrantedOnEntity(WithGrantRestrictions entityWithGrantRestrictions,
            ProvolyUser user,
            boolean withWriteAccess) {

        log.debugf("Validating if user %s can %s %s with id=%s",
                user.getId(),
                withWriteAccess ? "write" : "see",
                entityWithGrantRestrictions.getClass().getSimpleName(),
                entityWithGrantRestrictions.getId());

        if (user.isAdmin() || user.equals(entityWithGrantRestrictions.getOwner())) {
            log.debugf("User %s is admin or owner of the %s (owner=%s)",
                    user.getId(),
                    entityWithGrantRestrictions.getClass().getSimpleName(),
                    entityWithGrantRestrictions.getOwner().getId());
            return true;
        }

        log.debugf("User is neither admin or owner of the %s, checking group access ...",
                entityWithGrantRestrictions.getClass().getSimpleName());
        Optional<Group> usersGroupWithWriteAccessOnEntity = groupRepository
                .getGroupsByEntityId(entityWithGrantRestrictions.getId())
                .stream()
                .filter(g -> !withWriteAccess || g.canWrite)
                .map(GroupRelations::getGroup)
                .filter(g -> user.getGroups().contains(g))
                .findFirst();

        if (usersGroupWithWriteAccessOnEntity.isPresent()) {
            log.debugf("User can access %s whit id=%s from group %s.",
                    entityWithGrantRestrictions.getClass().getSimpleName(),
                    entityWithGrantRestrictions.getId(),
                    usersGroupWithWriteAccessOnEntity.get().getName());
            return true;
        }

        return false;
    }

    public void canWrite(WithGrantRestrictions entityWithGrantRestrictions, ProvolyUser user) {
        if (!isUserGrantedOnEntity(entityWithGrantRestrictions, user, true)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User is not granted to write %s with id=%s.".formatted(
                            entityWithGrantRestrictions.getClass().getSimpleName(),
                            entityWithGrantRestrictions.getId()));
        }

    }

    public boolean canSee(WithGrantRestrictions entityWithGrantRestrictions, ProvolyUser user) {
        return isUserGrantedOnEntity(entityWithGrantRestrictions, user, false);
    }

    @Transactional
    public <T extends EntityId> List<T> getAllUserAllowed(WithGroupEntityType type, ProvolyUser user) {
        if (!user.isAdmin()) {
            log.infof("Get %s for user %s with groups %s", type.getEntity().getSimpleName(), user.getId(),
                    user.getGroups().stream().map(Group::getName).toList());
            return groupRepository.getAllowedEntityId(user, type.getEntity());
        }

        log.infof("Admin user, getting all %s", type.getEntity().getSimpleName());
        return entityIdRepository.getAll(type.getEntity());
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
}
