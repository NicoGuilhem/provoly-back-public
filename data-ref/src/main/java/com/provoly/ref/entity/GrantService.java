package com.provoly.ref.entity;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Path;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.UserDto;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.groups.*;
import com.provoly.ref.user.UserService;

@ApplicationScoped
public class GrantService {

    private UserService userService;
    private EntityManager em;

    public GrantService(UserService userService, EntityManager em) {
        this.userService = userService;
        this.em = em;
    }

    public void canWrite(EntityNamed entityNamed, WithGroupEntityType type) {
        var canWrite = switch (type) {
            case DASHBOARD -> (((Dashboard) entityNamed).getUser().equals(userService.getCurrentUser()));
            case DATASET -> (((Dataset) entityNamed).getUser().equals(userService.getCurrentUser()));
        };

        if (!canWrite) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User is not granted to write %s %s.".formatted(type, entityNamed.getId()));
        }
    }

    public boolean canSee(EntityNamed entityNamed, WithGroupEntityType type, UserDto user) {
        HashSet<UUID> userGroupsId = getUserGroupsId(user);

        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(GroupRelations.class);
        Path<UUID> idPath = root.get(GroupRelations_.GROUP_ID);
        q = q.select(idPath);
        q = q.where(cb.and(
                cb.equal(root.get(GroupRelations_.entityType), type),
                cb.equal(root.get(GroupRelations_.entityId), entityNamed.getId())));
        List<UUID> entityGrantedGroups = em.createQuery(q).getResultList();

        boolean isUserOwner = switch (type) {
            case DASHBOARD -> (((Dashboard) entityNamed).getUser().equals(userService.getCurrentUser()));
            case DATASET -> (((Dataset) entityNamed).getUser().equals(userService.getCurrentUser()));
        };
        return isUserOwner
                || isEntityNotPrivate(entityGrantedGroups) && entityGrantedGroups.stream().anyMatch(userGroupsId::contains);
    }

    private boolean isEntityNotPrivate(List<UUID> groupsByEntityId) {
        return groupsByEntityId != null;
    }

    private HashSet<UUID> getUserGroupsId(UserDto user) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(Group.class);
        Path<UUID> idPath = root.get(Group_.ID);
        q = q.select(idPath);
        q = q.where(
                cb.in(root.get(Group_.NAME)).value(user.getGroups()));
        return em.createQuery(q).getResultStream().collect(Collectors.toCollection(HashSet::new));
    }
}
