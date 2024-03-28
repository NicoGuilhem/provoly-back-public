package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.relation.RelationTypeDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/relation-types")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface RelationTypeService {

    @POST
    void addRelationType(RelationTypeDto relationType);

    @DELETE
    @Path("/id/{id}")
    void deleteRelationType(@PathParam("id") UUID id);
}
