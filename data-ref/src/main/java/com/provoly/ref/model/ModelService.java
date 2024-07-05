package com.provoly.ref.model;

import static java.lang.Math.min;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.category.Category;
import com.provoly.ref.category.CategoryService;
import com.provoly.ref.category.WithCategoryEntityType;
import com.provoly.ref.customclass.CustomClassService;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.model.field.Field;
import com.provoly.ref.model.field.Field_;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ModelService {

    private EntityManager em;
    private ModelMapper modelMapper;
    private RefEventService refEventService;
    private CustomClassService customClassService;
    private EntityIdRepository entityIdRepository;
    private AssociationService associationService;
    private MetadataService metadataService;
    private CategoryService categoryService;
    @ConfigProperty(name = "provoly.ref.storages")
    List<Storage> storagesProperties;

    public ModelService(EntityManager em,
            ModelMapper modelMapper,
            RefEventService refEventService,
            CustomClassService customClassService,
            EntityIdRepository entityIdRepository,
            AssociationService associationService,
            MetadataService metadataService, CategoryService categoryService) {
        this.em = em;
        this.modelMapper = modelMapper;
        this.refEventService = refEventService;
        this.customClassService = customClassService;
        this.entityIdRepository = entityIdRepository;
        this.associationService = associationService;
        this.metadataService = metadataService;
        this.categoryService = categoryService;
    }

    public void saveCategory(Category category) {
        category.setWithCategoryEntityType(WithCategoryEntityType.ATTRIBUTES);
        categoryService.save(category);
    }

    @Transactional
    public void saveClass(OClassWriteDto newClass) {
        OClass oclass = modelMapper.toModel(newClass);
        verifyDuplicateAttributeTechnicalName(oclass);
        verifyAttributesName(oclass);
        verifyIdsAlreadyUsedInOtherOClass(oclass);
        verifyStorages(newClass);

        var oldClass = entityIdRepository.findById(newClass.getId(), OClass.class);

        if (oldClass == null) {
            saveNewClass(newClass, oclass);
            return;
        }
        updateOclass(newClass, oldClass, oclass);
    }

    private void updateOclass(OClassWriteDto newClass, OClass oldClass, OClass oclass) {
        // Update an existing class
        if (oldClass.getStorage() != newClass.getStorage()) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Can't change model storage from %s to %s".formatted(oldClass.getStorage(), newClass.getStorage()));
        }
        verifyNotRemovingAttribute(oldClass, oclass); // For now, we are not supporting removing attributes
        entityIdRepository.saveEntity(oclass);
        if (!List.of(Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE).contains(newClass.getStorage())) {
            refEventService.classUpdated(oclass);
        }
        metadataService.updateMetadataByEntityType(newClass, EntityType.CLASS);
        newClass.getAttributes().forEach(this::processAttribute);
    }

    private void saveNewClass(OClassWriteDto newClass, OClass oclass) {
        // Create a new class
        entityIdRepository.saveEntity(oclass);
        if (!List.of(Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE).contains(newClass.getStorage())) {
            refEventService.classCreated(oclass);
        }
        if (newClass.getMetadata() != null) {
            newClass.getMetadata()
                    .forEach(metadata -> metadataService.addMetadataToEntity(newClass.getId(), metadata.getMetadataDefId(),
                            metadata, EntityType.CLASS));
        }
        newClass.getAttributes().forEach(this::processAttribute);
    }

    private void verifyDuplicateAttributeTechnicalName(OClass oclass) {
        Set<String> technicalNames = new HashSet<>();
        oclass.getAttributes().stream().map(AttributeDef::getTechnicalName).forEach(att -> {
            if (technicalNames.contains(att)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "The technical name %s appears in several attributes".formatted(att));
            }
            technicalNames.add(att);
        });
    }

    private void processAttribute(AttributeDefDto attributeDefDto) {
        if (attributeDefDto.getCategory() != null) {
            categoryService.updateEntityCategories(List.of(attributeDefDto.getCategory()),
                    attributeDefDto.getId(), WithCategoryEntityType.ATTRIBUTES);
        }
        categoryService.updateEntityCategories(List.of(), attributeDefDto.getId(),
                WithCategoryEntityType.ATTRIBUTES);
    }

    private void verifyStorages(OClassWriteDto newClass) {
        if (!storagesProperties.contains(newClass.getStorage())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Storage %s is not defined as a storage in your configuration".formatted(newClass.getStorage()));
        }
    }

    private void verifyAttributesName(OClass oclass) {
        oclass.getAttributes().stream()
                .filter(attribute -> attribute.getName() == null)
                .forEach(attribute -> attribute
                        .setName(attribute.getTechnicalName().substring(0, min(50, attribute.getTechnicalName().length()))));
    }

    @Transactional
    public List<OClass> getClassByField(UUID id) {
        entityIdRepository.checkEntityExists(id, Field.class);
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(OClass.class);
        var field = q.from(OClass.class).join(OClass_.attributes).join(AttributeDef_.field);
        q.where(cb.equal(field.get(Field_.id), id));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public void addOrUpdateAttribute(UUID oClassId, AttributeDefDto attributeDefDto) {
        OClass oClass = entityIdRepository.getLinkedById(oClassId, OClass.class);
        getAttributeInSet(attributeDefDto, oClass.getAttributes()).ifPresentOrElse(
                oldAttributeDef -> {
                    oldAttributeDef.setName(attributeDefDto.getName());
                    oldAttributeDef.setTechnicalName(attributeDefDto.getTechnicalName());
                    if (attributeDefDto.getCategory() != null) {
                        categoryService.updateEntityCategories(List.of(attributeDefDto.getCategory()),
                                attributeDefDto.getId(), WithCategoryEntityType.ATTRIBUTES);
                    } else {
                        categoryService.updateEntityCategories(List.of(), attributeDefDto.getId(),
                                WithCategoryEntityType.ATTRIBUTES);
                    }
                },
                () -> oClass.addAttribute(modelMapper.toModel(attributeDefDto)));
        refEventService.classUpdated(oClass);
    }

    @Transactional
    public void deleteOClass(UUID oClassId) {
        var oClass = entityIdRepository.getLinkedById(oClassId, OClass.class);
        var oclassDto = modelMapper.toDetailsDto(oClass);
        deleteAssociatedEntities(oClassId);
        var associations = associationService.getClassAssociations(oClassId);
        if (associations.usedElsewhere() || !associations.associations().isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE,
                    "OClass contains one or more abac rules, links, datasets or namedqueries, remove them to delete the oClass %s"
                            .formatted(oclassDto.getName()));
        }

        oclassDto.getMetadata().forEach(metadata -> metadataService.deleteMetadataValueByEntityId(oClassId,
                metadata.getMetadataDef().id, EntityType.CLASS));

        oclassDto.getAttributes().forEach(attributeDefDetailsDto -> {
            categoryService.deleteAllByEntityId(attributeDefDetailsDto.getCategory());
        });
        em.remove(oClass);

        refEventService.classDeleted(oClass);
    }

    public List<CountDto> getDatasetCountByClass() {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CountDto.class);
        var dataset = q.from(Dataset.class);
        q.groupBy(dataset.get(Dataset_.oClass).get(OClass_.id));
        q.multiselect(dataset.get(Dataset_.oClass).get(OClass_.id).as(UUID.class),
                cb.count(dataset).as(Integer.class));

        return em.createQuery(q).getResultList();
    }

    public void removeEntity(EntityId id) {
        entityIdRepository.removeEntity(id);
    }

    public void saveEntity(EntityId id) {
        entityIdRepository.saveEntity(id);
    }

    public <T extends EntityId> boolean exists(T entity) {
        return entityIdRepository.exists(entity);
    }

    public Category getCategoryById(UUID id) {
        return entityIdRepository.getById(id, Category.class);
    }

    public OClass getOClassById(UUID id) {
        return entityIdRepository.getById(id, OClass.class);
    }

    public List<OClass> getAllOClasses() {
        return entityIdRepository.getAll(OClass.class);
    }

    public void removeCategoryEntity(UUID id) {
        if (categoryService.isCategoryUsedByAnyEntity(id)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You're not allowed to delete category %s".formatted(id));
        }
        entityIdRepository.removeEntity(id, Category.class);
    }

    @Transactional
    public void deleteAttributeById(UUID oclassId, UUID attributeId) {
        var associations = associationService.getAttributeAssociations(attributeId);
        if (!associations.associations().isEmpty() || associations.usedElsewhere()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "The attribute %s is used by one or more search(es) and abac rule(s), remove them to delete the attribute"
                            .formatted(attributeId));
        }
        var oclass = entityIdRepository.getById(oclassId, OClass.class);
        var attributeDef = entityIdRepository.getById(attributeId, AttributeDef.class);
        oclass.getAttributes()
                .stream()
                .map(EntityId::getId)
                .filter(entityIds -> entityIds == attributeId)
                .forEach(entityId -> categoryService.deleteAllByEntityId(entityId));
        oclass.deleteAttribute(attributeDef);
        refEventService.classUpdated(oclass);
    }

    public List<Storage> getStorages() {
        return storagesProperties;
    }

    private void deleteAssociatedEntities(UUID oClassId) {
        // remove customClass
        customClassService.deleteByoClassId(oClassId);
    }

    private Optional<AttributeDef> getAttributeInSet(AttributeDefDto attributeDefDto, Set<AttributeDef> attributeDefSet) {
        return attributeDefSet.stream()
                .filter(attributeDef -> attributeDef.getId().equals(attributeDefDto.getId()))
                .findFirst();
    }

    private void verifyIdsAlreadyUsedInOtherOClass(OClass oClass) {
        Set<AttributeDef> allOtherAttributes = entityIdRepository.getAll(OClass.class)
                .stream()
                .filter(oc -> !oc.getId().equals(oClass.getId()))
                .map(OClass::getAttributes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        for (AttributeDef attributeDef : oClass.getAttributes()) {
            getAttributeInSet(modelMapper.toDto(attributeDef), allOtherAttributes).ifPresent(otherAttributeDef -> {
                throw new BusinessException(ErrorCode.ID_ALREADY_USED,
                        "Attribute definition id %s is used by %s and cannot be assigned to %s"
                                .formatted(attributeDef.getId(), otherAttributeDef.getSlug(), attributeDef.getSlug()));
            });
        }
    }

    private void verifyNotRemovingAttribute(OClass oldClass, OClass newClass) {
        for (AttributeDef oldAttribute : oldClass.getAttributes()) {
            var newAttribute = newClass.getAttributeById(oldAttribute.getId())
                    .orElseThrow(
                            () -> new BusinessException(ErrorCode.BAD_REQUEST, "Missing attribute " + oldAttribute.getId()));
            if (newAttribute.getType() != oldAttribute.getType()) {
                String msg = "Changing field type is not supported for attribute :" + oldAttribute.getId() + " "
                        + oldAttribute.getName();
                msg += " " + oldAttribute.getType().getName() + "->" + oldAttribute.getType().getName();
                throw new BusinessException(ErrorCode.BAD_REQUEST, msg);
            }
        }
    }
}
