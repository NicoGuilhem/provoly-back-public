package com.provoly.ref.groups;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdService;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GroupService {
    private EntityIdService entityIdService;
    private GroupMapper groupMapper;
    private Logger log;
    @PersistenceContext
    private EntityManager em;

    public GroupService(EntityIdService entityIdService, GroupMapper groupMapper, EntityManager em,
            Logger log) {
        this.entityIdService = entityIdService;
        this.groupMapper = groupMapper;
        this.em = em;
        this.log = log;
    }

    public void addGroup(GroupWrite groupWrite) {
        entityIdService.saveEntity(groupMapper.toModel(groupWrite));
    }

    public Group getById(UUID groupId) {
        return entityIdService.getById(groupId, Group.class);
    }

    public void updateEntityGroups(List<String> groupsName, UUID entityId,
            WithGroupEntityType entityType) {
        if (groupsName == null) {
            log.debugf("No group provided for %s entity %s", entityType.name(), entityId);
            return;
        }
        getGroupsByEntityId(entityId).forEach(groupRelations -> {
            log.debugf("Remove group %s association to %s %s", groupRelations.getId(), entityType, entityId);
            deleteGroupFromEntity(entityId, groupRelations.getId());
        });
        associateGroupToEntity(groupsName, entityId, entityType);
    }

    @Transactional
    public void associateGroupToEntity(List<String> groupsName, UUID entityId,
            WithGroupEntityType entityType) {
        groupsName.forEach(name -> {
            var groupId = getGroupByName(name).getId();

            if (!isGroupAssignedToEntity(entityId, groupId)) {
                log.debugf("Associate group %s to %s %s", groupId, entityType, entityId);
                switch (entityType) {
                    case WithGroupEntityType.DASHBOARD -> entityIdService
                            .saveEntity(new DashboardGroupRelations(UUID.randomUUID(), groupId, entityId));
                    case WithGroupEntityType.DATASET -> entityIdService
                            .saveEntity(new DatasetGroupRelations(UUID.randomUUID(), groupId, entityId));
                }
            }
        });
    }

    private Group getGroupByName(String groupName) {
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

    private void deleteGroupFromEntity(UUID entityId, UUID groupId) {
        GroupRelations groupRelations = getGroupRelationsByEntityIdAndGroupId(entityId, groupId);

        em.remove(em.merge(groupRelations));
    }

    public List<Group> getGroupsByEntityId(UUID entityId) {
        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT gd.id FROM group_def as gd " +
                        "LEFT JOIN group_relations as gr ON gd.id = gr.group_id " +
                        "WHERE gr.entity_id = :entity_id ) "
                        + "SELECT * FROM group_def where id in (SELECT id FROM ids)",
                Group.class)
                .setParameter("entity_id", entityId)
                .getResultList();
    }

    public List<Group> getAll() {
        return entityIdService.getAll(Group.class);
    }

    private GroupRelations getGroupRelationsByEntityIdAndGroupId(UUID entityId, UUID groupId) {
        try {
            var cb = em.getCriteriaBuilder();
            var q = cb.createQuery(GroupRelations.class);
            var rootQuery = q.from(GroupRelations.class);
            q.where(cb.and(
                    cb.equal(rootQuery.get(GroupRelations_.entityId), entityId),
                    cb.equal(rootQuery.get(GroupRelations_.groupId), groupId)));
            return em.createQuery(q).getSingleResult();
        } catch (NoResultException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Group %s is not assigned to %s".formatted(groupId, entityId));
        }

    }

    private boolean isGroupAssignedToEntity(UUID entityId, UUID groupId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(GroupRelations.class);
        q.select(cb.count(rootQuery));
        q.where(cb.and(
                cb.equal(rootQuery.get(GroupRelations_.entityId), entityId),
                cb.equal(rootQuery.get(GroupRelations_.groupId), groupId)));
        return em.createQuery(q).getSingleResult() > 0;
    }
}
