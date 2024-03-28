package com.provoly.ref.dataset;

import java.util.List;

import jakarta.inject.Inject;

import com.provoly.ref.groups.Group;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.user.UserService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DatasetGroupMapper {
    @Inject
    GroupService groupService;
    @Inject
    UserService userService;

    @AfterMapping
    void getGroupsOfDataset(Dataset dataset, @MappingTarget DatasetDetailsDto datasetDto) {
        List<String> groupNames = groupService.getGroupsByEntityId(dataset.getId())
                .stream()
                .map(Group::getName)
                .toList();

        datasetDto.setGroups(groupNames);
        datasetDto.setOwner(dataset.getUser() != null && userService.isCurrentUser(dataset.getUser()));
    }
}
