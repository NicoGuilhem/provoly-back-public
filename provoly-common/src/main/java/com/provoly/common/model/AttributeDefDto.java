package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AttributeDefDto {
    private UUID id;
    private String name;
    private String technicalName;
    private UUID field;
    private UUID category;
    private boolean multiValued;
    private String slug;

    public AttributeDefDto() {
    }

    @Default
    @JsonCreator
    public AttributeDefDto(UUID id, String name, String technicalName, UUID field, UUID category, boolean multiValued,
            String slug) {
        this.id = id;
        this.name = name;
        this.technicalName = technicalName;
        this.field = field;
        this.category = category;
        this.multiValued = multiValued;
        this.slug = slug;
    }

    public AttributeDefDto(UUID id, String name, String technicalName, UUID field) {
        this.id = id;
        this.name = name;
        this.technicalName = technicalName;
        this.field = field;
        this.multiValued = false;
        this.category = null;
    }

    @Override
    public String toString() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public UUID getField() {
        return field;
    }

    public UUID getCategory() {
        return category;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public String getSlug() {
        return slug;
    }

    public void setCategory(UUID category) {
        this.category = category;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setField(UUID field) {
        this.field = field;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

}
