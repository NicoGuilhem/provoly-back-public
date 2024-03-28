package com.provoly.ref.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.*;
import com.provoly.common.user.Role;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.metadata.MetadataService;

import org.jboss.logging.Logger;

@Path("/model")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModelController {
    private static final UUID DEFAULT_CATEGORY_ID = UUID.fromString("cf666d66-838f-4d92-a4d2-a315df21fac9");
    private ModelService modelService;
    private ModelMapper mapper;
    private Logger logger;
    private AssociationService associationService;
    private MetadataService metadataService;

    public ModelController(ModelService modelService, ModelMapper mapper, Logger logger, AssociationService associationService,
            MetadataService metadataService) {
        this.modelService = modelService;
        this.mapper = mapper;
        this.logger = logger;
        this.associationService = associationService;
        this.metadataService = metadataService;
    }

    @PUT
    @Path("/categories")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void addCategory(CategoryDto categoryDto) {
        modelService.saveCategory(mapper.toModel(categoryDto));
    }

    @DELETE
    @Path("/categories/{id}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void deleteCategory(UUID id)
            throws ProvolyNotFoundException {
        if (id.equals(DEFAULT_CATEGORY_ID)) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE, "This is default category id. It can't be deleted");
        }
        modelService.removeCategoryEntity(id);
    }

    @GET
    @Path("/categories/{id}")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public CategoryDto getCategory(UUID id)
            throws ProvolyNotFoundException {
        return mapper.toDetailsDto(modelService.getCategoryById(id));
    }

    @GET
    @Path("/categories")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<CategoryDto> getCategories() {
        return mapper.toCategoryDto(modelService.getAllCategories());
    }

    @POST
    @Path("/field")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void addField(FieldDto field) {
        modelService.addFields(Collections.singletonList(field));
    }

    @POST
    @Path("/fields")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void addFields(Collection<FieldDto> fields) throws ProvolyNotFoundException {
        modelService.addFields(fields);
    }

    @GET
    @Path("/fields")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<FieldDto> getFields() {
        return mapper.toFieldDto(modelService.getAllFields());
    }

    @GET
    @Path("/fields/{id}")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_ITEM_WRITE, Role.STR_SEARCH })
    public FieldDto getFieldById(UUID id) {
        return mapper.toDto(modelService.getFieldById(id));
    }

    @DELETE
    @Path("/fields/{id}")
    @RolesAllowed({ Role.STR_FIELD_WRITE })
    public void deleteFieldById(UUID id) {
        modelService.deleteFieldById(id);
    }

    @GET
    @Path("/fields/class/{id}")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_SEARCH, Role.STR_ITEM_WRITE, Role.STR_DATASOURCE_READ })
    public Collection<FieldDto> getFieldsForClass(UUID id) {
        return mapper.toFieldDto(modelService.getFieldForClass(id));
    }

    @GET
    @Path("/class")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public Collection<OClassReadDto> get() {
        return mapper.toClassReadDto(modelService.getAllOClasses());
    }

    @GET
    @Path("/class/field/{id}")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public Collection<OClassDetailsDto> getByField(UUID id) {
        return mapper.toClassDetailsDto(modelService.getClassByField(id));
    }

    @POST
    @Path("/class")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void saveClass(OClassWriteDto oclassDto)
            throws BusinessException {
        logger.infof("Receive request to save Oclass with name %s", oclassDto.getName());
        modelService.saveClass(oclassDto);
    }

    @GET
    @Path("/class/id/{id}")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public OClassReadDto getById(UUID id)
            throws ProvolyNotFoundException {
        return mapper.toClassReadDto(modelService.getOClassById(id));
    }

    @GET
    @Path("/class/id/{id}/details")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    public OClassDetailsDto getDetails(UUID id)
            throws ProvolyNotFoundException {
        return mapper.toDetailsDto(modelService.getOClassById(id));
    }

    @DELETE
    @Path("/class/id/{id}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void deleteClass(UUID id)
            throws ProvolyNotFoundException {
        modelService.deleteOClass(id);
    }

    @PUT
    @Path("/class/id/{id}/attribute")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void addAttribute(UUID id, AttributeDefDto attributeDefDto) {
        modelService.addOrUpdateAttribute(id, mapper.toModel(attributeDefDto));
    }

    @GET
    @Path("/class/datasets/count")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public List<CountDto> getDatasetCountByClass() {
        return modelService.getDatasetCountByClass();
    }

    @DELETE
    @Path("/class/id/{classId}/attribute/id/{attributeId}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void deleteAttribute(@PathParam("classId") UUID classId, @PathParam("attributeId") UUID attributeId) {
        modelService.deleteAttributeById(classId, attributeId);
    }

    @PUT
    @Path("/id/{classId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void setMetadataToClass(UUID classId, UUID metadataDefId, MetadataValueWriteDto metadata) {
        metadataService.addMetadataToEntity(classId, metadataDefId, metadata, EntityType.CLASS);
    }

    @DELETE
    @Path("/id/{classId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public void deleteMetadataToClass(UUID classId, UUID metadataDefId) {
        metadataService.deleteMetadataValueByEntityId(classId, metadataDefId, EntityType.CLASS);
    }

    @GET
    @Path("/fields/{id}/associations")
    @RolesAllowed({ Role.STR_FIELD_READ, Role.STR_FIELD_WRITE })
    public AssociationsDto getAssociationOfField(UUID id) {
        return associationService.getFieldAssociations(id);
    }

    @GET
    @Path("/attribute/{id}/associations")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public AssociationsDto getAttributeAssociations(UUID id) {
        return associationService.getAttributeAssociations(id);
    }

    @GET
    @Path("/class/{id}/associations")
    @RolesAllowed({ Role.STR_CLASS_READ })
    public AssociationsDto getClassAssociations(@PathParam("id") UUID id) {
        return associationService.getClassAssociations(id);
    }

    @GET
    @Path("/storages")
    @RolesAllowed({ Role.STR_CLASS_WRITE })
    public List<Storage> getStorages() {
        return modelService.getStorages();
    }
}
