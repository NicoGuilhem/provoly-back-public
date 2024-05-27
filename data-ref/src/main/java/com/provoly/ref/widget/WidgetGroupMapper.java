package com.provoly.ref.widget;

import java.util.List;

import jakarta.inject.Inject;

import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.user.UserService;
import com.provoly.ref.widget.dto.WidgetDetailsDto;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class WidgetGroupMapper {
    @Inject
    GroupRepository groupRepository;
    @Inject
    UserService userService;

    @AfterMapping
    void getWidgetGroups(WidgetCatalog widgetCatalog, @MappingTarget WidgetDetailsDto widgetDto) {
        List<String> groupNames = groupRepository.getGroupsByEntityId(widgetCatalog.getId())
                .stream()
                .map(groupRelation -> groupRelation.getGroup().getName())
                .toList();

        widgetDto.setGroups(groupNames);
        widgetDto.setOwner(userService.isCurrentUser(widgetCatalog.getUser()));
    }
}