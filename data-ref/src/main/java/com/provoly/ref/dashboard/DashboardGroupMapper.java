package com.provoly.ref.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.provoly.common.dataset.GroupRights;
import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.groups.GroupRelations;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.user.UserService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DashboardGroupMapper {
    @Inject
    GroupRepository groupRepository;
    @Inject
    UserService userService;

    @AfterMapping
    void getDashboardGroups(Dashboard dashboard, @MappingTarget DashboardReadDto dashboardReadDto) {
        Map<String, List<GroupRights>> accessRightsByGroup = groupRepository.getGroupsByEntityId(dashboard.getId())
                .stream()
                .collect(Collectors.toMap(groupRelations -> groupRelations.getGroup().getName(), this::createRights));

        dashboardReadDto.setAccessRightsByGroup(accessRightsByGroup);
        dashboardReadDto.setOwner(userService.isCurrentUser(dashboard.getUser()));

    }

    private List<GroupRights> createRights(GroupRelations groupRelations) {
        var rights = new ArrayList<GroupRights>();
        rights.add(GroupRights.READ);
        if (groupRelations.canWrite()) {
            rights.add(GroupRights.WRITE);
        }
        return rights;
    }
}
