package com.provoly.common.model;

import java.util.UUID;

public class AttributeDefDto {
    public UUID id;
    public String name;
    public String technicalName;
    public UUID field;
    public UUID category;
    public boolean multiValued;
    public String slug;

    public AttributeDefDto() {
    }

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

    @Override
    public String toString() {
        return name;
    }
}
