package com.provoly.ref.abac.predicate;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.abac.PredicateDto;
import com.provoly.common.user.Role;
import com.provoly.ref.abac.AbacMapper;

@Path("/predicates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PredicateController {

    @Inject
    PredicateService predicateService;

    @Inject
    AbacMapper abacMapper;

    @GET
    @RolesAllowed({ Role.STR_DATA_ACCESS_READ })
    public Collection<PredicateDto> getAllPredicates() {
        return abacMapper.toCollectionPredicateDto(predicateService.getAllPredicates());
    }

    @POST
    @RolesAllowed({ Role.STR_DATA_ACCESS_WRITE })
    public void addPredicate(PredicateDto predicateDto) {
        predicateService.save(abacMapper.toModel(predicateDto));
    }

    @GET
    @RolesAllowed({ Role.STR_DATA_ACCESS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{id}")
    public PredicateDto getPredicate(UUID id) {
        return abacMapper.toDto(predicateService.getPredicate(id));
    }

}
