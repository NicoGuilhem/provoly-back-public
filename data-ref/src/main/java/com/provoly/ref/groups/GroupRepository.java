package com.provoly.ref.groups;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.*;
import com.provoly.ref.user.ProvolyUser;

@ApplicationScoped
@Transactional
public class GroupRepository {

    @PersistenceContext
    private EntityManager em;
    private EntityIdRepository entityIdRepository;

    public GroupRepository(EntityIdRepository entityIdRepository, EntityManager em) {
        this.entityIdRepository = entityIdRepository;
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
        entityIdRepository.saveEntity(entityType.buildGroupRelations(UUID.randomUUID(), group, entityId, canWrite));
    }

    public void save(Group group) {
        entityIdRepository.saveEntity(group);
    }

    public Group getById(UUID groupId) {
        return entityIdRepository.getById(groupId, Group.class);
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
        return entityIdRepository.getAll(Group.class);
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

    /**
     * Builds a subquery selecting entity ids of GroupRelations from a list of Group's names
     * 
     * @param query the query we want the subquery
     * @param groupsList the list of Group
     * @return Subquery<UUID>
     */
    private Subquery<UUID> selectEntityIdsFromGroupListNames(CriteriaQuery<?> query, List<Group> groupsList) {

        Subquery<UUID> subqueryGroupRelations = query.subquery(UUID.class);
        Root<GroupRelations> fromSubQuery = subqueryGroupRelations.from(GroupRelations.class);
        Join<GroupRelations, Group> groupDef = fromSubQuery.join(GroupRelations_.group);
        Predicate groupRelationsOfGroups = groupDef.get(EntityNamed_.name)
                .in(groupsList.stream().map(EntityNamed::getName).toList());

        subqueryGroupRelations
                .select(fromSubQuery.get(GroupRelations_.entityId))
                .where(groupRelationsOfGroups);

        return subqueryGroupRelations;
    }

    public <T extends EntityId> List<T> getAllowedEntityId(final @NotNull ProvolyUser user, final Class<T> entityId) {
        return getAllowedEntityId(user, entityId, null, null);
    }

    public <T extends EntityId> List<T> getAllowedEntityId(final @NotNull ProvolyUser user,
            final Class<T> entityId,
            final CriteriaQueryOptionsFunction<T> criteriaQueryOptionsFunction) {
        return getAllowedEntityId(user, entityId, criteriaQueryOptionsFunction, null);
    }

    public <T extends EntityId> List<T> getAllowedEntityId(final @NotNull ProvolyUser user,
            final Class<T> entityId,
            final CriteriaQueryOptionsFunction<T> criteriaQueryOptionsFunction,
            final Consumer<TypedQuery<T>> additionalTypedQueryModifier) {

        return getAllowedEntityId(user, entityId, criteriaQueryOptionsFunction, additionalTypedQueryModifier,
                null);
    }

    public <T extends EntityId> List<T> getAllowedEntityId(final @NotNull ProvolyUser user,
            final Class<T> entityId,
            final CriteriaQueryOptionsFunction<T> criteriaQueryOptionsFunction,
            final Consumer<TypedQuery<T>> additionalTypedQueryModifier,
            final SupplierFromUserEntity getFromUserAllowed) {
        final var cb = em.getCriteriaBuilder();
        final var query = cb.createQuery(entityId);

        final var root = query.from(entityId);

        final Predicate userIsOwnerOfEntity;
        final Predicate userHasGroupOfEntity;

        if (getFromUserAllowed != null) {
            var fromUserAllowed = getFromUserAllowed.get(query, root);
            userIsOwnerOfEntity = cb.equal(fromUserAllowed.get("user"), user);
            userHasGroupOfEntity = fromUserAllowed.get(EntityId_.id)
                    .in(this.selectEntityIdsFromGroupListNames(query, user.getGroups()));
        } else {
            userIsOwnerOfEntity = cb.equal(root.get("user"), user);
            userHasGroupOfEntity = root.get(EntityId_.id)
                    .in(this.selectEntityIdsFromGroupListNames(query, user.getGroups()));
        }

        final Predicate allowedEntity = cb.or(userHasGroupOfEntity, userIsOwnerOfEntity);

        Predicate filters = allowedEntity;
        if (criteriaQueryOptionsFunction != null) {
            Predicate additionalFilters = criteriaQueryOptionsFunction.build(cb, query, root);
            if (additionalFilters != null) {
                filters = cb.and(additionalFilters, allowedEntity);
            }
        }
        query.select(root).where(filters);

        var typedQuery = em.createQuery(query);

        if (additionalTypedQueryModifier != null) {
            additionalTypedQueryModifier.accept(typedQuery);
        }

        return typedQuery.getResultList();
    }

    public <T extends EntityId> long countAllowedEntityId(final @NotNull ProvolyUser user,
            final Class<T> entityId,
            final CriteriaCountQueryOptionsFunction<T> criteriaQueryOptionsFunction,
            final SupplierFromUserEntity getFromUserAllowed) {
        final var cb = em.getCriteriaBuilder();
        final var q = cb.createQuery(Long.class);

        final var root = q.from(entityId);

        var fromUserAllowed = getFromUserAllowed.get(q, root);
        final Predicate userIsOwnerOfEntity = cb.equal(fromUserAllowed.get("user"), user);
        final Predicate userHasGroupOfEntity = fromUserAllowed.get(EntityId_.id)
                .in(this.selectEntityIdsFromGroupListNames(q, user.getGroups()));

        final Predicate allowedEntity = cb.or(userHasGroupOfEntity, userIsOwnerOfEntity);

        Predicate filters = allowedEntity;
        if (criteriaQueryOptionsFunction != null) {
            Predicate additionalFilters = criteriaQueryOptionsFunction.build(cb, q, root);
            if (additionalFilters != null) {
                filters = cb.and(additionalFilters, allowedEntity);
            }
        }
        q.select(cb.count(root)).where(filters);

        var typesQuery = em.createQuery(q);

        return typesQuery.getSingleResult();
    }

    @FunctionalInterface
    public interface CriteriaQueryOptionsFunction<T extends EntityId> {
        public Predicate build(CriteriaBuilder cb, CriteriaQuery<T> q, Root<T> root);
    }

    @FunctionalInterface
    public interface CriteriaCountQueryOptionsFunction<T extends EntityId> {
        public Predicate build(CriteriaBuilder cb, CriteriaQuery<Long> q, Root<T> root);
    }

    @FunctionalInterface
    public interface SupplierFromUserEntity<R extends From<?, ? extends EntityId>> {
        public R get(CriteriaQuery<?> q, Root<?> root);
    }
}
