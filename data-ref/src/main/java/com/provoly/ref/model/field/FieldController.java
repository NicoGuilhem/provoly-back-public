package com.provoly.ref.model.field;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.model.field.FieldDto;
import com.provoly.common.user.Role;
import com.provoly.ref.model.ModelMapper;

@Path("/model")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FieldController {

    private ModelMapper mapper;
    private FieldService fieldService;

    public FieldController(ModelMapper mapper, FieldService fieldService) {
        this.mapper = mapper;
        this.fieldService = fieldService;
    }

    @POST
    @Path("/fields")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void addField(FieldDto field) {
        fieldService.addField(field);
    }

    @PUT
    @Path("/fields/id/{id}")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void updateField(@PathParam("id") UUID id, FieldDto field) {
        fieldService.updateField(id, field);
    }

    @GET
    @Path("/fields")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<FieldDto> getFields() {
        return mapper.toFieldDto(fieldService.getAllFields());
    }

    @GET
    @Path("/fields/{id}")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public FieldDto getFieldById(UUID id) {
        return mapper.toDto(fieldService.getFieldById(id));
    }

    @DELETE
    @Path("/fields/{id}")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void deleteFieldById(UUID id) {
        fieldService.deleteFieldById(id);
    }

    @GET
    @Path("/fields/class/{id}")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_SEARCH, Role.STR_ITEM_WRITE, Role.STR_DATASOURCE_READ })
    public Collection<FieldDto> getFieldsForClass(UUID id) {
        return mapper.toFieldDto(fieldService.getFieldForClass(id));
    }
}
