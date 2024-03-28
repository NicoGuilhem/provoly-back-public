package com.provoly.virt.item;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.user.Role;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.search.mono.MonoMapper;

import org.jboss.logging.Logger;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemsController {

    @Inject
    Logger log;

    @Inject
    MonoMapper mapper;

    @Inject
    WriteItemsService itemsService;

    @Inject
    ReadItemsService getItemsService;

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void insert(List<ItemDto> items) {
        long start = System.currentTimeMillis();
        var errors = itemsService.addItemsDto(items);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Add items fail : " + errors);
        }

        log.infof("Inserted %d items in %sms", items.size(), System.currentTimeMillis() - start);

    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{id}")
    public ItemDto get(
            ItemId id) {
        return mapper.toDto(getItemsService.get(id));
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{id}/relations")
    public ItemsSearchResultDto searchWithRelations(
            ItemId id) {
        return mapper.toDto(getItemsService.searchRelationsFromItem(id));
    }

}