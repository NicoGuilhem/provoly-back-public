package com.provoly.virt.item;

import java.util.List;
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
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.user.Role;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.search.mono.MonoMapper;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

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
    public void insert(List<ItemDto> items) {
        long start = System.currentTimeMillis();
        if (items.getFirst() == null) {
            log.info("No items to insert");
            return;
        }

        String datasetVersionId = items.getFirst().getDatasetVersionId(); // Items presences checked above
        DatasetDto datasetDto = datasetService
                .searchByDatasetVersionId(UUID.fromString(datasetVersionId));

        canAdd(datasetDto, UUID.fromString(datasetVersionId));

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

    private void canAdd(DatasetDto dataset, UUID datasetVersionDtoId) {
        if (dataset.getType().equals(DatasetType.CLOSED)) {
            DatasetVersionDetailsDto datasetVersionDto = datasetVersionService.get(datasetVersionDtoId);
            if (!datasetVersionDto.getState().equals(DatasetState.INDEXING)) {
                throw new BusinessException(ErrorCode.NOT_MODIFIABLE,
                        "Dataset %s is not in INDEXING state".formatted(dataset.getName()));
            }
        }
    }

}