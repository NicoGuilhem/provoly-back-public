package com.provoly.virt.item;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.clients.DatasetService;
import com.provoly.clients.DatasetVersionService;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.user.Role;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.search.mono.MonoMapper;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemsController {

    Logger log;
    MonoMapper mapper;
    WriteItemsService itemsService;
    DatasetService datasetService;
    DatasetVersionService datasetVersionService;
    ReadItemsService getItemsService;

    public ItemsController(MonoMapper mapper, WriteItemsService itemsService, @RestClient DatasetService datasetService,
            @RestClient DatasetVersionService datasetVersionService, ReadItemsService getItemsService, Logger log) {
        this.mapper = mapper;
        this.itemsService = itemsService;
        this.datasetService = datasetService;
        this.datasetVersionService = datasetVersionService;
        this.getItemsService = getItemsService;
        this.log = log;
    }

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void insertOrUpdate(List<ItemDto> items,
            @DefaultValue("REPLACE") @RestQuery("updateMode") ItemUpdateMode updateMode) {
        long start = System.currentTimeMillis();
        try {
            items.getFirst();
        } catch (NoSuchElementException e) {
            log.info("No items to insert or update");
            return;
        }

        String datasetVersionId = items.getFirst().getDatasetVersionId(); // Items presences checked above
        DatasetDto datasetDto = datasetService
                .searchByDatasetVersionId(UUID.fromString(datasetVersionId));

        canAddOrUpdate(datasetDto, UUID.fromString(datasetVersionId));

        var errors = itemsService.addOrUpdateItemsDto(items, updateMode);
        if (!errors.isEmpty()) {
            if (errors.size() == 1 && errors.getFirst().cause() != null) {
                throw errors.getFirst().cause();
            }
            throw new IllegalStateException("Add or update items failed : " + errors);
        }

        log.infof("Inserted or updated %d items in %sms", items.size(), System.currentTimeMillis() - start);

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

    private void canAddOrUpdate(DatasetDto dataset, UUID datasetVersionDtoId) {
        if (dataset.getType().equals(DatasetType.CLOSED)) {
            DatasetVersionDetailsDto datasetVersionDto = datasetVersionService.get(datasetVersionDtoId);
            if (!datasetVersionDto.getState().equals(DatasetState.INDEXING)) {
                throw new BusinessException(ErrorCode.NOT_MODIFIABLE,
                        "Dataset %s is not in INDEXING state".formatted(dataset.getName()));
            }
        }
    }

}