package com.provoly.virt.entity;

import java.util.*;
import java.util.stream.Collectors;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassDetailsDto;

public class Item {

    private final ItemId id;
    private final OClassDetailsDto oClass;
    private final Map<String, AttributeDefDetailsDto> attributesDef = new HashMap<>();
    private Map<UUID, MetadataValueDto> metadata = null; // Metadata for object
    private final Map<String, AttributeValue> attributes = new HashMap<>();

    public Item(ItemId id, OClassDetailsDto oClass, Collection<FieldDto> fields) {
        this.id = id;

        oClass.getAttributes().forEach(attr -> attributesDef.put(attr.getTechnicalName(), attr));
        this.oClass = new OClassDetailsDto(oClass.getId(), oClass.getSlug(), oClass.getName(), null,
                attributesDef.values().stream().toList(), oClass.getStorage(), List.of());
    }

    public Item(ItemId id, OClassDetailsDto oClass) {
        this.id = id;
        this.oClass = oClass;
        oClass.getAttributes().forEach(attr -> attributesDef.put(attr.getTechnicalName(), attr));
    }

    public String getIdAsString() {
        return id.getAsString();
    }

    public UUID getDatasetVersion() {
        return id.getDatasetVersionId();
    }

    public void add(MetadataValueDto metadataValueDto) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(metadataValueDto.getMetadataId(), metadataValueDto);
    }

    public MetadataValueDto getMetadata(UUID metadataId) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(metadataId);
    }

    public Iterable<MetadataValueDto> getMetadata() {
        if (metadata == null) {
            return Collections.emptyList();
        }
        return metadata.values();
    }

    public AttributeSimpleValue getAttributeSimple(String technicalName) {
        // TODO : Check attributeDef is simple attribute
        var attribute = (AttributeSimpleValue) attributes.get(technicalName);
        if (attribute == null) {
            AttributeDefDetailsDto attributeDef = getAttributeDefByTechnicalName(technicalName);
            attribute = new AttributeSimpleValue(this, attributeDef);
            attributes.put(technicalName, attribute);
        }
        return attribute;
    }

    public AttributeMultiValue getAttributeMulti(String technicalName) {
        // TODO : Check attributeDef is multi attribute
        var attribute = (AttributeMultiValue) attributes.get(technicalName);
        if (attribute == null) {
            AttributeDefDetailsDto attributeDef = getAttributeDefByTechnicalName(technicalName);
            attribute = new AttributeMultiValue(this, attributeDef);
            attributes.put(technicalName, attribute);
        }
        return attribute;
    }

    AttributeDefDetailsDto getAttributeDefByTechnicalName(String technicalName) {
        var attrDef = attributesDef.get(technicalName);
        if (attrDef == null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Attribute " + technicalName + " not found in " + oClass.getName());
        }
        return attrDef;
    }

    public <T extends AttributeValue> T getAttributeByTechnicalName(String name, Class<T> clazz) {
        return (T) attributes.get(name);
    }

    public <T extends AttributeValue> Iterable<T> getAttributes(Class<T> clazz) {
        return attributes.values().stream()
                .filter(attr -> attr.getClass().equals(clazz))
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    public Iterable<AttributeValue> getAttributesValue() {
        return attributes.values();
    }

    public boolean isAttributeMultivalued(String attributeName) {
        return getAttributeDefByTechnicalName(attributeName).isMultiValued();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", oClass=" + oClass.getName() +
                '}';
    }

    public ItemId getId() {
        return id;
    }

    public OClassDetailsDto getoClass() {
        return oClass;
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }

}
