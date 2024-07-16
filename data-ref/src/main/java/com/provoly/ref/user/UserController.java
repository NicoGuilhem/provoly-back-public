package com.provoly.ref.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.metadata.UserMetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.user.Role;
import com.provoly.common.user.UserDto;
import com.provoly.ref.user.metadata.UserProfileMapper;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    private UserService userService;
    private UserProfileMapper userProfileMapper;

    public UserController(UserService userService, UserProfileMapper userProfileMapper) {
        this.userService = userService;
        this.userProfileMapper = userProfileMapper;
    }

    @GET
    @Path("/me")
    @RolesAllowed("**")
    public UserDto getCurrentUserInfo() {
        return userService.getCurrentUserDto();
    }

    @GET
    @Path("/me/metadata")
    @RolesAllowed({ Role.STR_METADATA_USER_REF_READ, Role.STR_METADATA_USER_READ, Role.STR_SEARCH })
    public Collection<UserProfileValueReadDto> getMines() {
        ProvolyUser user = userService.getCurrentUser();
        return userService.getUserProfileValueReadDtos(user.getId());
    }

    @GET
    @RolesAllowed({ Role.STR_USER_READ })
    public List<UserDto> getAllUsers() {
        return userProfileMapper.toUserDtos(userService.getAll());
    }

    @GET
    @Path("/id/{userId}/metadata")
    @RolesAllowed(Role.STR_METADATA_USER_READ)
    public List<UserProfileValueReadDto> getProfileValuesByUserId(UUID userId) {
        return userService.getUserProfileValueReadDtos(userId);
    }

    @PUT
    @Path("/id/{userId}/metadata/id/{metadataDefId}")
    @RolesAllowed(Role.STR_METADATA_USER_WRITE)
    public void addProfilesForUser(UUID userId, UUID metadataDefId, UserMetadataValueWriteDto metadatas) {
        userService.addUserProfiles(userId, metadataDefId, metadatas);
    }

    @DELETE
    @Path("/id/{userId}/metadata/id/{metadataDefId}")
    @RolesAllowed(Role.STR_METADATA_USER_WRITE)
    public void deleteProfileForUser(UUID userId, UUID metadataDefId) {
        userService.deleteProfileForUser(userId, metadataDefId);
    }

}
