package com.provoly.virt.search;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.search.SearchRequestDto;
import com.provoly.common.search.SortDto;
import com.provoly.common.user.Role;
import com.provoly.virt.search.mono.MonoMapper;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/items/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchController {

    @Inject
    MonoMapper mapper;

    @Inject
    SearchService searchService;

    @POST
    @RolesAllowed({ Role.STR_SEARCH })
    public ItemsSearchResultDto search(
            SearchRequestDto request,
            @RestQuery("order") SortDto sort) {
        var result = mapper.toDto(searchService.search(request, sort));
        return new ItemsSearchResultDto(result, request.getGeoFormat());
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/named/{nameQueryId}")

    public ItemsSearchResultDto searchByNamedQuery(
            UUID nameQueryId,
            @RestQuery("order") SortDto sort,
            @QueryParam("limit") int limit) {
        return mapper.toDto(searchService.searchByNamedQuery(nameQueryId, sort, limit));
    }

}