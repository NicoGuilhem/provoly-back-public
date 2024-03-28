package com.provoly.common.model;

import java.util.UUID;

public class AttributeDefDetailsDto {

    public UUID oclass;
    public UUID id;
    public String name;
    public String technicalName;
    public String slug;
    public boolean multiValued;
    public FieldDto field;

    public AttributeDefDetailsDto() {
    }

    public AttributeDefDetailsDto(UUID oclass, AttributeDefDto attr, FieldDto field) {
        this.oclass = oclass;
        this.id = attr.id;
        this.name = attr.name;
        this.technicalName = attr.technicalName;
        this.slug = attr.slug;
        this.multiValued = attr.multiValued;
        this.field = field;
    }

    @Override
    public String toString() {
        return "AttributeDef{" +
                ", id=" + id +
                "oClass=" + oclass +
                ", name='" + name + '\'' +
                "}";
    }
}
