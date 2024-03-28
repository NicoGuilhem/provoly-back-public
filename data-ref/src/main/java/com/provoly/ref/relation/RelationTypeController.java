package com.provoly.ref.relation;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.relation.RelationTypeDetailsDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.user.Role;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/relation-types")
public class RelationTypeController {

    @Inject
    RelationTypeMapper mapper;

    @Inject
    RelationTypeService relationTypeService;

    @POST
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void save(RelationTypeDto relationTypeDto) {
        relationTypeService.saveOrUpdate(relationTypeDto);
    }

    @GET
    @RolesAllowed({ Role.STR_CLASS_READ })
    @Path("/id/{id}")
    public RelationTypeDetailsDto getById(UUID id) {
        return mapper.toDtoDetails(relationTypeService.getById(id));
    }

    @GET
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<RelationTypeDto> getAll() {
        return mapper.toClassDto(relationTypeService.getAll());
    }

    @DELETE
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    @Path("/id/{id}")
    public void delete(UUID id) {
        relationTypeService.delete(id);
    }

}
