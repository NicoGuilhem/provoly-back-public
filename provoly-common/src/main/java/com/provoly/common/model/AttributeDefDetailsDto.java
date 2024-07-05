package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.model.field.FieldDto;

public class AttributeDefDetailsDto {

    private UUID oclass;
    private UUID id;
    private String name;
    private String technicalName;
    private String slug;
    private boolean multiValued;
    private FieldDto field;
    private UUID category;

    public AttributeDefDetailsDto() {
    }

    public AttributeDefDetailsDto(UUID oclass, AttributeDefDto attr, FieldDto field, UUID category) {
        this.oclass = oclass;
        this.id = attr.getId();
        this.name = attr.getName();
        this.technicalName = attr.getTechnicalName();
        this.slug = attr.getSlug();
        this.multiValued = attr.isMultiValued();
        this.field = field;
        this.category = category;
    }

    @Override
    public String toString() {
        return "AttributeDef{" +
                ", id=" + id +
                "oClass=" + oclass +
                ", name='" + name + '\'' +
                "}";
    }

    public UUID getId() {
        return id;
    }

    public UUID getOclass() {
        return oclass;
    }

    public String getName() {
        return name;
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public String getSlug() {
        return slug;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setOclass(UUID oclass) {
        this.oclass = oclass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public FieldDto getField() {
        return field;
    }

    public void setField(FieldDto field) {
        this.field = field;
    }

    public UUID getCategory() {
        return category;
    }
}
