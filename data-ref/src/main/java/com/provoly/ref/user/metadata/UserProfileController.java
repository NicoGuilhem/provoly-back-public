package com.provoly.ref.user.metadata;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.metadata.UserProfileDto;
import com.provoly.common.user.Role;

@Path("/users/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserProfileController {
    private UserProfileService userProfileService;

    private UserProfileMapper mapper;

    public UserProfileController(UserProfileService userProfileService, UserProfileMapper mapper) {
        this.userProfileService = userProfileService;
        this.mapper = mapper;
    }

    @POST
    @RolesAllowed({ Role.STR_METADATA_USER_REF_WRITE })
    public void addUserProfile(@Valid UserProfileDto userProfileDto) {
        userProfileService.saveUserProfile(mapper.toModel(userProfileDto));
    }

    @GET
    @RolesAllowed({ Role.STR_METADATA_USER_REF_READ })
    public Collection<UserProfileDto> getAll() {
        return mapper.toUserProfileDtoList(userProfileService.getAll());
    }

    @GET
    @Path("/{metadataId}")
    @RolesAllowed({ Role.STR_METADATA_USER_REF_READ })
    public UserProfileDto getDefById(UUID metadataId) {
        return mapper.toUserProfileDto(userProfileService.getById(metadataId));
    }

}