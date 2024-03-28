package com.provoly.virt.metadata;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;
import com.provoly.virt.entity.ItemId;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetadataController {

    @Inject
    MetadataService metadataService;

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    @Path("/id/{itemId}/metadata/name/{metadataName}/{value}")
    public void setMetadataToItem(
            ItemId itemId,
            String metadataName,
            String value) {
        metadataService.addMetadataToItem(itemId, metadataName, value);
    }

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    @Path("/id/{itemId}/attributes/name/{attributeName}/metadata/name/{metadataName}/{value}")
    public void setMetadataToAttribute(
            ItemId itemId,
            String attributeName,
            String metadataName,
            String value) {
        metadataService.addMetadataToAttribute(itemId, attributeName, metadataName, value);
    }
}
