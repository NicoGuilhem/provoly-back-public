package com.provoly.ref.user;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.error.BusinessException;
import com.provoly.common.search.NamedQueryDetailsDto;
import com.provoly.common.search.NamedQueryDto;
import com.provoly.common.user.Role;

@Path("/users/me/namedquery")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamedQueryController {

    @Inject
    NamedQueryService namedQueryService;

    @Inject
    NamedQueryMapper mapper;

    @GET
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Transactional
    public Collection<NamedQueryDetailsDto> getMines() {
        return mapper.toNamedQueryDto(namedQueryService.getNamedQueriesForCurrentUser());
    }

    @POST
    @RolesAllowed({ Role.STR_SEARCH })
    public void saveNamedQuery(NamedQueryDto namedQuery) {
        namedQueryService.saveNamedQueryForUser(namedQuery);
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/{id}/info")
    @Transactional
    public NamedQueryDetailsDto getUserNamedQueryById(UUID id) {
        ProvolyUserNamedQuery provolyUserNamedQuery = namedQueryService.getMineById(id);
        return mapper.toDto(provolyUserNamedQuery.getNamedQuery(), provolyUserNamedQuery);
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATA_VIRT })
    @Path("/{id}")
    @Transactional
    public NamedQueryDetailsDto getNamedQueryById(UUID id) {
        try {
            ProvolyUserNamedQuery provolyUserNamedQuery = namedQueryService.getMineById(id);
            return mapper.toDto(provolyUserNamedQuery.getNamedQuery(), provolyUserNamedQuery);
        } catch (BusinessException ignored) {
            NamedQuery nq = namedQueryService.getById(id);
            return mapper.toDto(nq, null);
        }
    }

    @DELETE
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/{id}")
    public void deleteForMe(UUID id) {
        namedQueryService.delete(id);
    }

    @POST
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/{id}/favorite")
    public void addToMineFavorite(UUID id) {
        namedQueryService.addToMineFavorite(id);
    }

    @DELETE
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/{id}/favorite")
    public void removeFromMineFavorite(UUID id) {
        namedQueryService.removeFromMineFavorite(id);
    }

    @POST
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/{id}/execution")
    public void updateNamedQueryExecution(UUID id) {
        namedQueryService.updateLastExecutionDate(id);
    }
}
