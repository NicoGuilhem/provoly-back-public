package com.provoly.ref.link;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.link.LinkDetailsDto;
import com.provoly.common.link.LinkDto;
import com.provoly.common.user.Role;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/links")
public class LinkController {

    @Inject
    LinkMapper mapper;

    @Inject
    LinkService linkService;

    @GET
    @RolesAllowed({ Role.STR_LINK_READ, Role.STR_UPDATE_RELATION_AGGREGATE })
    @Path("/id/{id}")
    public LinkDetailsDto getById(UUID id) {
        return mapper.toDto(linkService.getById(id));
    }

    @POST
    @RolesAllowed({ Role.STR_LINK_WRITE })
    public void saveLink(LinkDto linkDto) {
        linkService.save(mapper.toModel(linkDto));
    }

    @DELETE
    @RolesAllowed({ Role.STR_LINK_WRITE })
    @Path("/id/{id}")
    public void deleteLink(UUID id) {
        linkService.delete(id);
    }

    @GET
    @RolesAllowed({ Role.STR_LINK_READ })
    public Collection<LinkDetailsDto> getAll() {
        return mapper.toClassDto(linkService.getAll());
    }
}
