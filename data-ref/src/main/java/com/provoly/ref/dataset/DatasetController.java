package com.provoly.ref.dataset;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.CategoryDto;
import com.provoly.common.user.Role;
import com.provoly.ref.category.CategoryMapper;
import com.provoly.ref.category.CategoryService;
import com.provoly.ref.category.WithCategoryEntityType;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionMapper;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.groups.GroupErrors;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.AssociationsDto;

import org.jboss.resteasy.reactive.RestQuery;
import org.jetbrains.annotations.NotNull;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetController {

    private DatasetService datasetService;
    private DatasetMapper datasetMapper;
    private DatasetVersionMapper datasetVersionMapper;
    private MetadataService metadataService;
    private DatasetVersionRepository datasetVersionRepository;
    private CategoryMapper categoryMapper;
    private CategoryService categoryService;

    public DatasetController(DatasetService datasetService,
            DatasetMapper datasetMapper,
            DatasetVersionMapper datasetVersionMapper, MetadataService metadataService,
            DatasetVersionRepository datasetVersionRepository, CategoryMapper categoryMapper, CategoryService categoryService) {
        this.datasetService = datasetService;
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.metadataService = metadataService;
        this.datasetVersionRepository = datasetVersionRepository;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
    }

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ })
    public Collection<DatasetDetailsDto> getAll() {
        List<Dataset> datasets = datasetService.getAll();
        return toDatasetDetailsDtoWithActiveVersionComputed(datasets);
    }

    @GET
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_DATASET_READ, Role.STR_SEARCH })
    public DatasetDetailsDto get(UUID id) {
        DatasetDetailsDto datasetDetailsDto = datasetMapper.toDatasetDetailsDto(datasetService.getById(id));
        datasetVersionRepository.getByDatasetId(id).map(datasetVersionMapper::toDto)
                .ifPresent(datasetDetailsDto::setActiveVersion);
        return datasetDetailsDto;
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
        DatasetDetailsDto datasetDetailsDto = datasetMapper.toDatasetDetailsDto(datasetService.getByName(name));
        datasetVersionRepository.getByDatasetId(datasetDetailsDto.getId()).map(datasetVersionMapper::toDto)
                .ifPresent(datasetDetailsDto::setActiveVersion);
        return datasetDetailsDto;
    }

    //FIXME /datasets/search n'est pas documenté dans l'openapi
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
        return datasetVersionMapper.toDatasetVersionDetailsDto(datasetVersionRepository.getByDatasetId(datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "No dataset version found for dataset %s.".formatted(datasetId))));
    }

    @GET
    @Path("/class/{id}")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH })
    public Collection<DatasetDetailsDto> getAllForClass(UUID id) {
        Collection<Dataset> datasets = datasetService.getAllClassAllowedDatasets(id);
        return toDatasetDetailsDtoWithActiveVersionComputed(datasets);
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

    @GET
    @RolesAllowed({ Role.STR_DATASET_READ })
    @Path("/categories")
    public List<CategoryDto> getAllCategories() {
        return categoryMapper.toCategoriesDtoList(categoryService.getAll(WithCategoryEntityType.DATASET));
    }

    @PUT
    @RolesAllowed({ Role.STR_DATASET_WRITE })
    @Path("/categories")
    public void addCategory(CategoryDto categoryDto) {
        datasetService.addCategory(categoryDto);
    }

    private @NotNull Collection<DatasetDetailsDto> toDatasetDetailsDtoWithActiveVersionComputed(Collection<Dataset> datasets) {
        Collection<DatasetDetailsDto> datasetDetailsDtoList = datasetMapper.toDatasetDetailsDtoList(datasets);
        List<DatasetVersion> activeDatasetVersions = datasetService.getActiveVersionsOrderedByVersionNumberDesc(datasets);
        for (DatasetDetailsDto dataset : datasetDetailsDtoList) {
            activeDatasetVersions.stream()
                    .filter(version -> version.getDataset().getId().equals(dataset.getId()))
                    .findFirst()
                    .ifPresent(version -> {
                        DatasetVersionDto dtoWithoutDataset = datasetVersionMapper.toDtoWithoutDataset(version);
                        dtoWithoutDataset.setDataset(dataset.getId());
                        dtoWithoutDataset.setoClass(dataset.getoClass());
                        dataset.setActiveVersion(dtoWithoutDataset);
                    });
        }
        return datasetDetailsDtoList;
    }
}