package com.provoly.virt.entity;

import java.util.*;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;

public class Item {

    private final ItemId id;
    private final OClassDetailsDto oClass;
    private final Map<String, AttributeDefDetailsDto> attributesDef = new HashMap<>();
    private Map<UUID, MetadataValueDto> metadata = null; // Metadata for object
    private final Map<String, AttributeValue> attributes = new HashMap<>();

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
        var attribute = attributes.computeIfAbsent(technicalName, this::buildAttributeFromAttributeDefByTechnicalName);
        if (attribute instanceof AttributeSimpleValue attributeSimpleValue) {
            return attributeSimpleValue;
        }
        throw new BusinessException(ErrorCode.TECHNICAL,
                "Attribute " + technicalName + " is not a simple valued attribute.");
    }

    public AttributeMultiValue getAttributeMulti(String technicalName) {
        var attribute = attributes.computeIfAbsent(technicalName, this::buildAttributeFromAttributeDefByTechnicalName);
        if (attribute instanceof AttributeMultiValue attributeMultiValue) {
            return attributeMultiValue;
        }
        throw new BusinessException(ErrorCode.TECHNICAL,
                "Attribute " + technicalName + " is not a multi valued attribute.");
    }

    AttributeDefDetailsDto getAttributeDefByTechnicalName(String technicalName) {
        var attrDef = attributesDef.get(technicalName);
        if (attrDef == null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Attribute " + technicalName + " not found in " + oClass.getName());
        }
        return attrDef;
    }

    private AttributeValue buildAttributeFromAttributeDefByTechnicalName(String technicalName) {
        AttributeDefDetailsDto attributeDef = getAttributeDefByTechnicalName(technicalName);
        if (attributeDef.isMultiValued()) {
            return new AttributeMultiValue(this, attributeDef);
        } else {
            return new AttributeSimpleValue(this, attributeDef);
        }
    }

    public <T extends AttributeValue> T getAttributeByTechnicalName(String name, Class<T> clazz) {
        return (T) attributes.get(name);
    }

    public <T extends AttributeValue> Iterable<T> getAttributes(Class<T> clazz) {
        return attributes.values().stream()
                .filter(attr -> attr.getClass().equals(clazz))
                .map(clazz::cast)
                .toList();
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

    public List<String> getSortedAttributes() {
        return attributes.keySet().stream()
                .sorted()
                .toList();
    }
}
