package com.provoly.ref.dashboard;

import java.util.List;

import jakarta.inject.Inject;

import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.groups.Group;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.user.UserService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DashboardGroupMapper {
    @Inject
    GroupService groupService;
    @Inject
    UserService userService;

    @AfterMapping
    void getGroupsOfDashboard(Dashboard dashboard, @MappingTarget DashboardReadDto dashboardReadDto) {
        List<String> groupNames = groupService.getGroupsByEntityId(dashboard.getId())
                .stream()
                .map(Group::getName)
                .toList();

        dashboardReadDto.setGroups(groupNames);
        dashboardReadDto.setOwner(userService.isCurrentUser(dashboard.getUser()));

    }
}
