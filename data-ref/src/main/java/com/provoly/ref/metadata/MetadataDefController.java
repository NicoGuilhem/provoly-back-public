package com.provoly.ref.metadata;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.user.Role;

@Path("/metadata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetadataDefController {
    MetadataDefService metadataDefService;

    MetadataMapper mapper;

    public MetadataDefController(MetadataDefService metadataDefService, MetadataMapper mapper) {
        this.metadataDefService = metadataDefService;
        this.mapper = mapper;
    }

    @GET
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE, Role.STR_DATASET_WRITE, Role.STR_ITEM_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public Set<MetadataDefDto> getAll() {
        return new HashSet<>(mapper.toMetadataDto(metadataDefService.getAllMetadataDef()));
    }

    @GET
    @Path("/id/{metadataId}")
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_READ, Role.STR_ITEM_WRITE, Role.STR_SEARCH, Role.STR_DASHBOARD_READ })
    public MetadataDefDto getById(UUID metadataId) {
        return mapper.toDto(metadataDefService.getById(metadataId));
    }

    @GET
    @Path("/name/{metadataName}")
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_READ, Role.STR_ITEM_WRITE, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public MetadataDefDto getByName(String metadataName) {
        return mapper.toDto(metadataDefService.getByName(metadataName, MetadataDef.class)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Unknown metadata name : %s".formatted(metadataName))));

    }

    @GET
    @Path("/slug/{metadataSlug}")
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_READ, Role.STR_ITEM_WRITE, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public MetadataDefDto getBySlug(String metadataSlug) {
        return mapper.toDto(metadataDefService.getBySlug(metadataSlug, MetadataDef.class)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Unknown metadata with slug : %s".formatted(metadataSlug))));
    }

    @POST
    @RolesAllowed({ Role.STR_METADATA_ITEM_REF_WRITE })
    public void addMetadata(@Valid MetadataDefDto metadataDto) {
        metadataDefService.addMetadata(mapper.toModel(metadataDto));
    }

}
