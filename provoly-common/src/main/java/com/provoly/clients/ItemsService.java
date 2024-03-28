package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.search.Direction;
import com.provoly.common.search.SortType;

import io.smallrye.mutiny.Multi;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Provide datasource details from a dataset, a dataset definition or a request
 */
@Path("/data-sources")
@RegisterRestClient(configKey = "data-virt")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface ItemsService {

    @GET
    @Path("/id/{dataSourceId}/items")
    @Produces(MediaType.APPLICATION_JSON)
    ItemsSearchResultDto getItems(UUID dataSourceId);

    @GET
    @Path("/id/{dataSourceId}/items/paginate")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<ItemsSearchResultDto> getPaginateItems(UUID dataSourceId, @QueryParam("id") UUID id,
            @QueryParam("direction") Direction direction, @QueryParam("type") SortType type);

}
