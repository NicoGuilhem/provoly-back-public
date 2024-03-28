package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.search.NamedQueryDto;
import com.provoly.common.search.SearchRequestDto;
import com.provoly.common.user.UserDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/users")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface ProvolyUserService {

    @GET
    @Path("/me")
    UserDto getMe();

    @POST
    @Path("/me/namedquery")
    void saveNamedQuery(NamedQueryDto namedQuery);

    @GET
    @Path("/me/namedquery/{id}")
    NamedQueryDto getNamedQueryById(@PathParam("id") UUID id);

    @POST
    @Path("/me/namedquery/{id}/execution")
    void updateNamedQueryExecution(@PathParam("id") UUID id);

    @DELETE
    @Path("/me/namedquery/{id}")
    void deleteNamedQuery(@PathParam("id") UUID id);

    @GET
    @Path("/me/currentSearch")
    SearchRequestDto getCurrentSearch();

    @DELETE
    @Path("/me/currentSearch")
    void deleteCurrentSearch();

    @GET
    @Path("/me/metadata")
    Collection<UserProfileValueReadDto> getCurrentUserMetadata();

    @GET
    @Path("/metadata")
    Collection<UserProfileDto> getAllUserProfiles();

    @POST
    @Path("/metadata")
    void addUserProfile(UserProfileDto userProfileDto);

    @PUT
    @Path("/id/{userId}/metadata/id/{userProfileId}")
    void setUserProfile(@PathParam("userId") UUID userId, @PathParam("userProfileId") UUID metadataDefId,
            MetadataValueWriteDto metadata);

}