package com.provoly.ref.user.metadata;

import java.util.List;

import com.provoly.common.metadata.UserProfileDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.user.UserDto;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;
import com.provoly.ref.user.ProvolyUser;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, MetadataMapper.class, EntitySlugMapper.class })
public interface UserProfileMapper {
    List<UserProfileDto> toUserProfileDtoList(List<UserProfile> allEntityIds);

    @Mapping(source = "values", target = "allowedValues")
    UserProfileDto toUserProfileDto(UserProfile userProfile);

    @Mapping(source = "allowedValues", target = "values")
    UserProfile toModel(UserProfileDto userProfileDto);

    UserProfileValueReadDto toUserProfileValueDto(UserProfileValue userProfileValue, UserProfile userProfile);

    List<UserDto> toUserDtos(List<ProvolyUser> provolyUsers);

    @Mapping(source = "lastName", target = "familyName")
    UserDto toUserDto(ProvolyUser provolyUsers);

    default UserProfileAllowedValue toModel(String allowedValue) {
        return new UserProfileAllowedValue(allowedValue);
    }

    default String toDto(UserProfileAllowedValue value) {
        return value.getValue();
    }
}