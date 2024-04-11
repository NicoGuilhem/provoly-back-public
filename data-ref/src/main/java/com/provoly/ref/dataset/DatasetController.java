package com.provoly.ref.dataset;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.user.Role;
import com.provoly.ref.datasetversion.DatasetVersionDetailsDto;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.groups.GroupErrors;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.AssociationsDto;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetController {

    private DatasetService datasetService;
    private DatasetMapper datasetMapper;
    private DatasetVersionService datasetVersionService;
    private DatasetVersionMapper datasetVersionMapper;
    private MetadataService metadataService;
    private DatasetVersionRepository datasetVersionRepository;

    public DatasetController(DatasetService datasetService,
            DatasetMapper datasetMapper, DatasetVersionService datasetVersionService,
            DatasetVersionMapper datasetVersionMapper, MetadataService metadataService,
            DatasetVersionRepository datasetVersionRepository) {
        this.datasetService = datasetService;
        this.datasetMapper = datasetMapper;
        this.datasetVersionService = datasetVersionService;
        this.datasetVersionMapper = datasetVersionMapper;
        this.metadataService = metadataService;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ })
    public Collection<DatasetDetailsDto> getAll() {
        return datasetMapper.toDatasetDetailsDtoList(datasetService.getAll());
    }

    @GET
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_DATASET_READ, Role.STR_SEARCH })
    public DatasetDetailsDto get(UUID id) {
        return datasetMapper.toDatasetDetailsDto(datasetService.getById(id));
    }

    @GET
    @Path("/id/{id}/dataset-versions")
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASET_READ, Role.STR_DATASOURCE_READ })
    public Collection<DatasetVersionDetailsDto> getAllById(UUID id) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getAllByDatasetId(id));
    }

    @GET
    @Path("/name/{name}")
    @RolesAllowed({ Role.STR_DATASET_READ })
    public DatasetDetailsDto getByName(String name) {
        return datasetMapper.toDatasetDetailsDto(datasetService.getByName(name));
    }

    @GET
    @Path("/search")
    @RolesAllowed({ Role.STR_DATASET_READ })
    public DatasetDto searchByDatasetVersionId(@RestQuery("dataset-version-id") UUID datasetVersionId) {
        return datasetMapper.toDto(datasetService.searchByDatasetVersionId(datasetVersionId));
    }

    @GET
    @Path("/name/{datasetName}/dataset-version")
    @RolesAllowed({ Role.STR_DATASET_READ })
    public DatasetVersionDetailsDto getDatasetVersionByDatasetName(String datasetName) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getByName(datasetName));
    }

    @GET
    @Path("/id/{datasetId}/dataset-version")
    @RolesAllowed({ Role.STR_DATASET_READ })
    public DatasetVersionDetailsDto getDatasetVersionByDatasetId(UUID datasetId) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getByDatasetId(datasetId));
    }

    @GET
    @Path("/class/{id}")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH })
    public Collection<DatasetDetailsDto> getAllForClass(UUID id) {
        return datasetMapper.toDatasetDetailsDtoList(datasetService.getAllClassAllowedDatasets(id));
    }

    @POST
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void addDataset(DatasetDto datasetDto) {
        datasetService.save(datasetDto);
    }

    @DELETE
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void delete(UUID id) {
        datasetService.deleteDataset(id);
    }

    @PUT
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public GroupErrors update(DatasetDto datasetDto) {
        return datasetService.updateDataset(datasetDto);
    }

    @PUT
    @Path("/id/{datasetId}/metadata/id/{metadataDefId}")
    @RolesAllowed(Role.STR_DATASET_WRITE)
    public void setMetadata(UUID datasetId, UUID metadataDefId, MetadataValueWriteDto metadata) {
        metadataService.addMetadataToEntity(datasetId, metadataDefId, metadata, EntityType.DATASET);
    }

    @DELETE
    @Path("/id/{datasetId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    public void deleteMetadata(UUID datasetId, UUID metadataDefId) {
        metadataService.deleteMetadataValueByEntityId(datasetId, metadataDefId, EntityType.DATASET);
    }

    @GET
    @Path("/{id}/associations")
    @RolesAllowed(Role.STR_DATASET_READ)
    public AssociationsDto getAssociationsByDatasetId(@PathParam("id") UUID datasetId) {
        return datasetService.getDatasetAssociations(datasetId);
    }

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ, Role.STR_DATASET_WRITE })
    @Path("/{id}/dataset-versions/latest")
    public DatasetVersionDetailsDto getLastVersionCreated(UUID id) {
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getLastVersionCreated(id));
    }
}