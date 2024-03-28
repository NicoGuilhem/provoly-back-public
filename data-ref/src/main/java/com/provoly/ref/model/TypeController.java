package com.provoly.ref.model;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.model.TypeDto;
import com.provoly.common.user.Role;

@Path("/types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class TypeController {
    @Inject
    TypeService typeService;

    @GET
    @RolesAllowed({ Role.STR_CLASS_READ })
    public List<TypeDto> getAll() {
        return typeService.getAll();
    }
}
