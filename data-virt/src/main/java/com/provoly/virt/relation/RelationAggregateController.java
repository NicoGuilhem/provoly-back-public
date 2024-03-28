package com.provoly.virt.relation;

import java.util.Collection;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.common.user.Role;

@Path("/relations/aggregate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RelationAggregateController {

    @Inject
    RelationService relationService;

    @POST
    @RolesAllowed({ Role.STR_UPDATE_RELATION_AGGREGATE })
    public void updateAggregate(
            Collection<RelationsAggregateDto> relationsAggregate) {
        relationService.updateAggregate(relationsAggregate);
    }

}
