package com.provoly.ref.metaProvisioning;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.metadata.MetaProvisioningDto;
import com.provoly.common.metadata.MetaProvisioningReaderDto;
import com.provoly.common.user.Role;

@Path("/meta-provisioning")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetaProvisioningController {

    @Inject
    MetaProvisioningMapper mapper;

    @Inject
    MetaProvisioningService metaService;

    @GET
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_READ })
    public List<MetaProvisioningReaderDto> getAll() {
        return mapper.toDtoReaderList(metaService.getAllMetaprovisionings());
    }

    @GET
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_READ })
    @Path("/id/{id}")
    public MetaProvisioningReaderDto getById(UUID id) {
        return mapper.toDtoReader(metaService.getById(id));
    }

    @POST
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_WRITE })
    public void add(MetaProvisioningDto metaProvisioningDto) {
        metaService.saveOrUpdate(metaProvisioningDto);
    }

    @DELETE
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_WRITE })
    @Path("/id/{id}")
    public void delete(UUID id) {
        metaService.removeEntity(id);
    }
}
