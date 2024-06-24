package com.provoly.ref.dataset;

import java.util.List;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.user.UserService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DatasetGroupMapper {
    @Inject
    GroupRepository groupRepository;
    @Inject
    UserService userService;

    @AfterMapping
    void getDatasetGroups(Dataset dataset, @MappingTarget DatasetDetailsDto datasetDto) {
        List<String> groupNames = groupRepository.getGroupsByEntityId(dataset.getId())
                .stream()
                .map(groupRelation -> groupRelation.getGroup().getName())
                .toList();

        datasetDto.setGroups(groupNames);
        datasetDto.setOwner(dataset.getUser() != null && userService.isCurrentUser(dataset.getUser()));
    }
}
