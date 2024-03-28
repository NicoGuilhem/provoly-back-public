package com.provoly.virt.relation;

import java.util.Collection;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.relation.RelationDto;
import com.provoly.common.user.Role;

import org.jboss.logging.Logger;

@Path("/relations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RelationsController {

    @Inject
    Logger log;

    @Inject
    RelationService relationService;

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void saveRelations(
            Collection<RelationDto> relations) {
        long start = System.currentTimeMillis();
        relationService.saveRelations(relations);
        log.debugf("Updates %d relations in %dms", relations.size(), System.currentTimeMillis() - start); // TODO : Add informations
    }

    @DELETE
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void delete(
            RelationDto relation) {
        relationService.delete(relation);
    }
}
