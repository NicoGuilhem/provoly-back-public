package com.provoly.common.model;

import java.util.UUID;

public abstract class AttributeBaseDto {
    protected UUID id;
    protected String name;
    protected String technicalName;
    protected UUID category;
    protected boolean multiValued;
    protected String slug;

    public AttributeBaseDto() {
    }

    public AttributeBaseDto(UUID id, String name, String technicalName, UUID category, boolean multiValued,
            String slug) {
        this.id = id;
        this.name = name;
        this.technicalName = technicalName;
        this.category = category;
        this.multiValued = multiValued;
        this.slug = slug;
    }

    public AttributeBaseDto(UUID id, String name, String technicalName) {
        this.id = id;
        this.name = name;
        this.technicalName = technicalName;
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

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }
}
