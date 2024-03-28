package com.provoly.ref.abac;

import java.util.Collection;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.abac.ContextVariableDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.user.Role;

@Path("/abac/context")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetadataContextController {

    @Inject
    AbacService abacService;

    @Inject
    AbacMapper mapper;

    @PUT
    @RolesAllowed({ Role.STR_METADATA_CONTEXT_WRITE })
    public void addContextVariable(ContextVariableDto dto) {
        ContextVariable abac = mapper.toModel(dto);
        abac.setValue(dto.value);
        abacService.saveContextVariable(abac);
    }

    @GET
    @RolesAllowed({ Role.STR_METADATA_CONTEXT_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<ContextVariableDto> listContextVariables() {
        return mapper.toCollectionAbacVariableContextDto(abacService.getAllContextVariable());
    }

    @GET
    @Path("/{variableName}")
    @RolesAllowed({ Role.STR_METADATA_CONTEXT_READ })
    public ContextVariableDto getContextVariable(String variableName)
            throws BusinessException {
        return mapper.toDto(abacService.getContextVariable(variableName));
    }

    @DELETE
    @Path("/{variableName}")
    @RolesAllowed({ Role.STR_METADATA_CONTEXT_WRITE })
    public void deleteContextVariable(String variableName)
            throws BusinessException {
        abacService.deleteContextVariable(variableName);
    }

}
