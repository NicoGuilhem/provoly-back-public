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
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.customclass.CustomClassService;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.metadata.MetadataService;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ModelService {

    private EntityManager em;
    private ModelMapper modelMapper;
    private RefEventService refEventService;
    private CustomClassService customClassService;
    private EntityIdService entityIdService;
    private AssociationService associationService;
    private MetadataService metadataService;
    @ConfigProperty(name = "provoly.ref.storages")
    List<Storage> storagesProperties;

    public ModelService(EntityManager em,
            ModelMapper modelMapper,
            RefEventService refEventService,
            CustomClassService customClassService,
            EntityIdService entityIdService,
            AssociationService associationService,
            MetadataService metadataService) {
        this.em = em;
        this.modelMapper = modelMapper;
        this.refEventService = refEventService;
        this.customClassService = customClassService;
        this.entityIdService = entityIdService;
        this.associationService = associationService;
        this.metadataService = metadataService;
    }

    public void saveCategory(Category category) {
        verifyCategoryDuplicateName(category);
        entityIdService.saveEntity(category);
    }

    public void addFields(Collection<FieldDto> fields) {
        fields.forEach(fieldDto -> {
            fieldDto.checkAndExtractSRID();
            Field entity = modelMapper.toModel(fieldDto);
            entityIdService.saveEntity(entity);
            FieldDto fieldUpdated = modelMapper.toDto(entity); // Ugly hack : at least to set the slug
            refEventService.fieldAdded(fieldUpdated);
        });
    }

    @Transactional
    public void saveClass(OClassWriteDto newClass) {
        OClass oclass = modelMapper.toModel(newClass);
        verifyAttributesName(oclass);
        verifyIdsAlreadyUsedInOtherOClass(oclass);
        verifyStorages(newClass);

        var oldClass = entityIdService.findById(newClass.getId(), OClass.class);

        if (oldClass == null) {
            // Create a new class
            entityIdService.saveEntity(oclass);
            if (!List.of(Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE).contains(newClass.getStorage())) {
                refEventService.classCreated(oclass);
            }
            if (newClass.getMetadata() != null) {
                newClass.getMetadata()
                        .forEach(metadata -> metadataService.addMetadataToEntity(newClass.getId(), metadata.getMetadataDefId(),
                                metadata, EntityType.CLASS));
            }
        } else {
            // Update an existing class
            if (oldClass.getStorage() != newClass.getStorage()) {
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        "Can't change model storage from %s to %s".formatted(oldClass.getStorage(), newClass.getStorage()));
            }
            verifyNotRemovingAttribute(oldClass, oclass); // For now, we are not supporting removing attributes
            entityIdService.saveEntity(oclass);
            if (!List.of(Storage.KUZZLE_ASSET, Storage.KUZZLE_MEASURE).contains(newClass.getStorage())) {
                refEventService.classUpdated(oclass);
            }
            metadataService.updateMetadataByEntityType(newClass, EntityType.CLASS);
        }
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
        entityIdService.checkEntityExists(id, Field.class);
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(OClass.class);
        var field = q.from(OClass.class).join(OClass_.attributes).join(AttributeDef_.field);
        q.where(cb.equal(field.get(Field_.id), id));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public Collection<Field> getFieldForClass(UUID id) {
        entityIdService.checkEntityExists(id, OClass.class);
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Field.class);
        var classRoot = q.from(OClass.class);
        var fieldRoot = classRoot.join(OClass_.attributes).join(AttributeDef_.field);
        q.select(fieldRoot).distinct(true).where(cb.equal(classRoot.get(OClass_.id), id));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public void addOrUpdateAttribute(UUID oClassId, AttributeDef attributeDef) {
        OClass oClass = entityIdService.getLinkedById(oClassId, OClass.class);
        getAttributeInSet(attributeDef, oClass.getAttributes()).ifPresentOrElse(
                oldAttributeDef -> {
                    oldAttributeDef.setName(attributeDef.getName());
                    oldAttributeDef.setTechnicalName(attributeDef.getTechnicalName());
                    oldAttributeDef.setCategory(attributeDef.getCategory());
                },
                () -> oClass.addAttribute(attributeDef));
        refEventService.classUpdated(oClass);
    }

    @Transactional
    public void deleteOClass(UUID oClassId) {
        var oClass = entityIdService.getLinkedById(oClassId, OClass.class);
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
        entityIdService.removeEntity(id);
    }

    public void saveEntity(EntityId id) {
        entityIdService.saveEntity(id);
    }

    public <T extends EntityId> boolean exists(T entity) {
        return entityIdService.exists(entity);
    }

    public Category getCategoryById(UUID id) {
        return entityIdService.getById(id, Category.class);
    }

    public List<Category> getAllCategories() {
        return entityIdService.getAll(Category.class);
    }

    public Field getFieldById(UUID id) {
        return entityIdService.getById(id, Field.class);
    }

    public List<Field> getAllFields() {
        return entityIdService.getAll(Field.class);
    }

    public OClass getOClassById(UUID id) {
        return entityIdService.getById(id, OClass.class);
    }

    public List<OClass> getAllOClasses() {
        return entityIdService.getAll(OClass.class);
    }

    public void removeCategoryEntity(UUID id) {
        entityIdService.removeEntity(id, Category.class);
    }

    public void deleteFieldById(UUID id) {
        AssociationsDto associations = associationService.getFieldAssociations(id);

        if (!associations.associations().isEmpty() || associations.usedElsewhere()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "The field %s is used by one or more attributes, remove them to delete the field".formatted(id));
        }

        entityIdService.removeEntity(id, Field.class);
    }

    @Transactional
    public void deleteAttributeById(UUID oclassId, UUID attributeId) {
        var associations = associationService.getAttributeAssociations(attributeId);
        if (!associations.associations().isEmpty() || associations.usedElsewhere()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "The attribute %s is used by one or more search(es) and abac rule(s), remove them to delete the attribute"
                            .formatted(attributeId));
        }
        var oclass = entityIdService.getById(oclassId, OClass.class);
        var attributeDef = entityIdService.getById(attributeId, AttributeDef.class);
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

    private Optional<AttributeDef> getAttributeInSet(AttributeDef attribute, Set<AttributeDef> attributeDefSet) {
        return attributeDefSet.stream()
                .filter(attributeDef -> attributeDef.getId().equals(attribute.getId()))
                .findFirst();
    }

    private void verifyIdsAlreadyUsedInOtherOClass(OClass oClass) {
        Set<AttributeDef> allOtherAttributes = entityIdService.getAll(OClass.class)
                .stream()
                .filter(oc -> !oc.getId().equals(oClass.getId()))
                .map(OClass::getAttributes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        for (AttributeDef attributeDef : oClass.getAttributes()) {
            getAttributeInSet(attributeDef, allOtherAttributes).ifPresent(otherAttributeDef -> {
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

    private void verifyCategoryDuplicateName(Category category) {
        entityIdService.getAll(Category.class)
                .stream()
                .filter(c -> !category.getId().equals(c.getId()))
                .filter(c -> category.getName().equals(c.getName()))
                .findFirst()
                .ifPresent(param -> {
                    throw new BusinessException(ErrorCode.NAME_ALREADY_USED,
                            category.getName() + " category name already used");
                });
    }
}
