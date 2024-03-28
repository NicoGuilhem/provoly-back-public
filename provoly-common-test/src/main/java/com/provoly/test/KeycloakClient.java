package com.provoly.test;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "sso")
@Path("/realms/provoly/protocol/openid-connect/token")
@RegisterClientHeaders(KeycloakServiceHeader.class)
public interface KeycloakClient {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    KeycloakTokenResponse getToken(@FormParam("username") String username, @FormParam("password") String password,
            @FormParam("grant_type") String grant_type);

}
