package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.model.field.FieldDto;

public class AttributeDefDetailsDto extends AttributeBaseDto {

    private UUID oclass;
    private FieldDto field;

    public AttributeDefDetailsDto() {
    }

    public AttributeDefDetailsDto(UUID oclass, AttributeDefDto attr, FieldDto field, UUID category) {
        super(attr.getId(), attr.getName(), attr.getTechnicalName(), attr.getCategory(), attr.isMultiValued(), attr.getSlug());
        this.oclass = oclass;
        this.field = field;
    }

    @Override
    public String toString() {
        return "AttributeDef{" +
                ", id=" + this.id +
                "oClass=" + oclass +
                ", name='" + name + '\'' +
                "}";
    }

    public UUID getOclass() {
        return oclass;
    }

    public FieldDto getField() {
        return field;
    }

    public void setField(FieldDto field) {
        this.field = field;
    }
}
