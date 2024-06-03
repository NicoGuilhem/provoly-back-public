package com.provoly.ref.groups;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityNamed;

@ApplicationScoped
@Transactional
public class GroupRepository {

    @PersistenceContext
    private EntityManager em;
    private EntityIdService entityIdService;

    public GroupRepository(EntityIdService entityIdService, EntityManager em) {
        this.entityIdService = entityIdService;
        this.em = em;
    }

    public Group getGroupByName(String groupName) {
        try {
            var cb = em.getCriteriaBuilder();
            var q = cb.createQuery(Group.class);
            var rootQuery = q.from(Group.class);
            q.where(cb.equal(rootQuery.get(Group_.name), groupName));
            return em.createQuery(q).getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Group %s doesn't exist.".formatted(groupName));
        }
    }

    public void saveGroupRelation(@NotNull WithGroupEntityType entityType, Group group, UUID entityId, boolean canWrite) {
        entityIdService.saveEntity(entityType.buildGroupRelations(UUID.randomUUID(), group, entityId, canWrite));
    }

    public void save(Group group) {
        entityIdService.saveEntity(group);
    }

    public Group getById(UUID groupId) {
        return entityIdService.getById(groupId, Group.class);
    }

    public List<Group> getGroupByNames(Collection<String> groupNames) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Group.class);
        var rootQuery = q.from(Group.class);

        if (groupNames == null) {
            return List.of();
        }
        q.where(rootQuery.get(Group_.name).in(groupNames));
        return em.createQuery(q).getResultList();
    }

    public void deleteGroupFromEntity(UUID entityId, Group group) {
        GroupRelations groupRelations = getGroupRelationsByEntityIdAndGroupId(entityId, group);

        em.remove(em.merge(groupRelations));
    }

    public List<GroupRelations> getGroupsByEntityId(UUID entityId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(GroupRelations.class);
        var rootQuery = q.from(GroupRelations.class);
        q.where(cb.equal(rootQuery.get(GroupRelations_.entityId), entityId));
        return em.createQuery(q).getResultList();
    }

    public List<Group> getAll() {
        return entityIdService.getAll(Group.class);
    }

    private GroupRelations getGroupRelationsByEntityIdAndGroupId(UUID entityId, Group group) {
        try {
            var cb = em.getCriteriaBuilder();
            var q = cb.createQuery(GroupRelations.class);
            var rootQuery = q.from(GroupRelations.class);
            q.where(cb.and(
                    cb.equal(rootQuery.get(GroupRelations_.entityId), entityId),
                    cb.equal(rootQuery.get(GroupRelations_.group), group)));
            return em.createQuery(q).getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Group %s is not assigned to %s".formatted(group, entityId));
        }

    }

    public boolean isGroupAssignedToEntity(UUID entityId, Group group) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(GroupRelations.class);
        q.select(cb.count(rootQuery));
        q.where(cb.and(
                cb.equal(rootQuery.get(GroupRelations_.entityId), entityId),
                cb.equal(rootQuery.get(GroupRelations_.group), group)));
        return em.createQuery(q).getSingleResult() > 0;
    }

    public List<GroupRelations> getEntityGroups(WithGroupEntityType type, EntityNamed entityNamed) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(GroupRelations.class);
        var root = q.from(GroupRelations.class);
        q = q.where(cb.and(
                cb.equal(root.get(GroupRelations_.entityType), type),
                cb.equal(root.get(GroupRelations_.entityId), entityNamed.getId())));
        return em.createQuery(q).getResultList();
    }
}
