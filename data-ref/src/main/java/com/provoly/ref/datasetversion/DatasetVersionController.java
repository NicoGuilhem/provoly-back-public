package com.provoly.ref.datasetversion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.search.Direction;
import com.provoly.common.user.Role;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.metadata.MetadataService;

@Path("/dataset-versions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetVersionController {

    private static final String RESPONSE_HEADER_TOTAL_COUNT = "X-Total-Count";
    private DatasetVersionService datasetVersionService;
    private DatasetVersionMapper datasetVersionMapper;
    private MetadataService metadataService;
    private DatasetVersionRepository datasetVersionRepository;

    public DatasetVersionController(DatasetVersionService datasetVersionService, DatasetVersionMapper datasetVersionMapper,
            MetadataService metadataService, DatasetVersionRepository datasetVersionRepository) {
        this.datasetVersionService = datasetVersionService;
        this.datasetVersionMapper = datasetVersionMapper;
        this.metadataService = metadataService;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ })
    //FIXME only retrieve datasetVersion allowed to user (@see GrantService)
    public Response getAll(@QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("dateMax") String dateMaxString,
            @QueryParam("dateMin") String dateMinString,
            @QueryParam("dataset") UUID datasetId,
            @QueryParam("state") DatasetState state,
            @DefaultValue("DATE") @QueryParam("orderBy") DatasetVersionOrderBy orderBy,
            @DefaultValue("asc") @QueryParam("sortBy") Direction sortBy) {
        DatasetVersionGetAllParams params = new DatasetVersionGetAllParams(limit, offset,
                dateMaxString != null ? Instant.parse(dateMaxString) : null,
                dateMinString != null ? Instant.parse(dateMinString) : null,
                datasetId, state, orderBy, sortBy);
        Collection<DatasetVersionDetailsDto> datasetVersionsList = datasetVersionMapper
                .toDatasetVersionDetailsDto(datasetVersionService.getAll(params));
        long countAll = datasetVersionService.countAll(params);
        return Response.ok(datasetVersionsList)
                .header(RESPONSE_HEADER_TOTAL_COUNT, countAll)
                .build();
    }

    @GET
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_DATASET_READ, Role.STR_SEARCH })
    public DatasetVersionDetailsDto get(@NotNull UUID id) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getById(id));
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH })
    @Path("/class/{classId}")
    public Collection<DatasetVersionDetailsDto> getAllActiveForClass(UUID classId) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getAllActiveForClass(classId));
    }

    @DELETE
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public DatasetVersionDto delete(UUID id) {
        return datasetVersionMapper.toDto(datasetVersionService.deleteDatasetVersion(id));
    }

    @PUT
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    @Path("/id/{id}")
    public void update(UUID id, DatasetVersionInformationDto datasetVersion) {
        datasetVersionService.update(datasetVersionRepository.getById(id), datasetVersion);
    }

    @POST
    @Path("/id/{id}/activate")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void activate(UUID id) {
        datasetVersionService.activateDatasetVersion(id);
    }

    @POST
    @Path("/id/{id}/deactivate")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void deactivate(UUID id) {
        datasetVersionService.deactivateDatasetVersion(id);
    }

    @POST
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public void create(DatasetVersionDto datasetVersion) {
        datasetVersionService.createDatasetVersion(datasetVersionMapper.toModel(datasetVersion));
    }

    @PUT
    @Path("/id/{datasetVersionId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void setMetadata(@PathParam("datasetVersionId") UUID datasetVersionId,
            @PathParam("metadataDefId") UUID metadataDefId,
            MetadataValueWriteDto metadata) {
        metadataService.addMetadataToEntity(datasetVersionId, metadataDefId, metadata, EntityType.DATASET_VERSION);
    }

    @DELETE
    @Path("/id/{datasetVersionId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void deleteMetadata(@PathParam("datasetVersionId") UUID datasetVersionId,
            @PathParam("metadataDefId") UUID metadataDefId) {
        metadataService.deleteMetadataValueByEntityId(datasetVersionId, metadataDefId, EntityType.DATASET_VERSION);
    }

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ })
    @Path("/id/{datasetVersionId}/previews")
    public List<MessageDto> getDatasetVersionPreviews(UUID datasetVersionId) {
        return datasetVersionService.getDatasetVersionPreviewsDto(datasetVersionId);
    }

}
