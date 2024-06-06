package com.provoly.virt.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.FieldDto;

public class AttributeSimpleValue extends AttributeValue {

    private final Map<UUID, MetadataValueDto> metadata = new HashMap<>(); // Metadata for attribute
    private boolean visible = false;
    private Object value;

    AttributeSimpleValue(Item parent, AttributeDefDetailsDto attributeDef) {
        this(parent, attributeDef, null);
    }

    AttributeSimpleValue(Item parent, AttributeDefDetailsDto attributeDef, Object value) {
        super(parent, attributeDef);
        this.value = value;
    }

    public void add(MetadataValueDto metadata) {
        this.metadata.put(metadata.getMetadataId(), metadata);
    }

    public Object getValue() {
        return (visible ? value : null);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object readValueEvenIfNotVisible() {
        return value;
    }

    public MetadataValueDto getMetadata(UUID metadataId) {
        if (metadata.containsKey(metadataId)) {
            return metadata.get(metadataId);
        }
        return getParent().getMetadata(metadataId);
    }

    public Iterable<MetadataValueDto> getMetadata() {
        return metadata.values();
    }

    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }

    public FieldDto getField() {
        return getAttributeDef().getField();
    }

    public String getSlugField() {
        return getAttributeDef().getField().slug;
    }

    public boolean isVisible() {
        return visible;
    }
}
