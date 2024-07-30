package com.provoly.ref.abac;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.user.Role;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/abac")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AbacController {

    @Inject
    AbacService abacService;

    @Inject
    AbacMapper mapper;

    @GET
    @Path("/rules")
    @RolesAllowed({ Role.STR_DATA_ACCESS_READ, Role.STR_SEARCH })
    public Collection<AbacRuleDto> getRules(@RestQuery("type") AbacRuleType type) {
        return mapper.toRuleDto(abacService.getAllRules(type));
    }

    @GET
    @Path("/rules/id/{ruleId}")
    @RolesAllowed({ Role.STR_DATA_ACCESS_READ })
    public AbacRuleDto getRule(UUID ruleId) {
        return mapper.toDto(abacService.getRule(ruleId));
    }

    @GET
    @Path("/rules/class/{oClassId}")
    @RolesAllowed({ Role.STR_DATA_ACCESS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<AbacRuleDto> getRulesFor(UUID oClassId, @RestQuery("include-inactive") boolean includeInactive) {
        return mapper.toRuleDto(abacService.getAllForClass(oClassId, includeInactive));
    }

    @POST
    @Path("/rules")
    @RolesAllowed({ Role.STR_DATA_ACCESS_WRITE })
    public void addRule(AbacRuleDto filterDto) {
        abacService.save(mapper.toModel(filterDto));
    }

    @DELETE
    @Path("/rules/{ruleId}")
    @RolesAllowed({ Role.STR_DATA_ACCESS_WRITE })
    public void deleteRule(UUID ruleId) {
        abacService.deleteRule(ruleId);
    }

}
