package com.provoly.ref.entity;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.UserDto;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.groups.*;
import com.provoly.ref.user.UserService;

import com.speedment.jpastreamer.application.JPAStreamer;
import com.speedment.jpastreamer.field.collector.FieldCollectors;

@ApplicationScoped
public class GrantService {

    private UserService userService;
    private JPAStreamer jpaStreamer;

    public GrantService(UserService userService, JPAStreamer jpaStreamer) {
        this.userService = userService;
        this.jpaStreamer = jpaStreamer;
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
        List<UUID> entityGrantedGroups = jpaStreamer.stream(GroupRelations.class)
                .filter(GroupRelations$.entityType.equal(type))
                .filter(GroupRelations$.entityId.equal(entityNamed.id))
                .collect(FieldCollectors.groupingBy(GroupRelations$.entityId,
                        Collectors.mapping(GroupRelations::getGroupId, Collectors.toList())))
                .get(entityNamed.id);

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
        return jpaStreamer
                .stream(Group.class)
                .filter(Group$.name.in(user.getGroups()))
                .map(EntityId::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
