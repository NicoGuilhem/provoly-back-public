package com.provoly.virt.dataset;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.item.ItemDto;
import com.provoly.common.user.Role;
import com.provoly.virt.imports.ImportService;

import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetController {
    @Inject
    ImportService importService;

    @POST
    @ResponseStatus(202)
    @Path("/id/{datasetId}/dataset-versions/id/{id}")
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void importData(
            Collection<ItemDto> items,
            UUID id,
            UUID datasetId) {
        importService.runImportFromItems(datasetId, id, items);
    }
}
