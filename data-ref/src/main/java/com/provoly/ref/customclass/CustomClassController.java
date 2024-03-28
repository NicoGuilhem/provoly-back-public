package com.provoly.ref.customclass;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;

@Path("/customclass")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomClassController {

    private CustomClassService customclassService;
    private CustomClassMapper customClassMapper;

    public CustomClassController(CustomClassService customclassService, CustomClassMapper customClassMapper) {
        this.customclassService = customclassService;
        this.customClassMapper = customClassMapper;
    }

    @PUT
    @Path("/id/{id}/param/{domain}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void addCustomClass(UUID id, String domain, String content) {
        customclassService.addCustomClass(id, domain, content);
    }

    @GET
    @Path("/id/{id}/param/{domain}")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public String getCustomClass(UUID id, String domain) {
        return customclassService.getCustomClass(id, domain).getContent();
    }

    @GET
    @Path("/all/param")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public List<CustomClass> getAll() {
        return customclassService.getAllCustomClass();
    }

    @GET
    @Path("/all/param/{domain}")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public List<String> getByDomain(String domain) {
        return customClassMapper.toListOfContent(customclassService.getAllCustomClassByDomain(domain));
    }
}
