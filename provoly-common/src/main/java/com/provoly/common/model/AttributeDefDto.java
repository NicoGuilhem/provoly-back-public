package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.model.field.FieldDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AttributeDefDto extends AttributeBaseDto {
    private FieldDto field;

    public AttributeDefDto() {
    }

    @Default
    @JsonCreator
    public AttributeDefDto(UUID id, String name, String technicalName, FieldDto field, UUID category, boolean multiValued,
            String slug) {
        super(id, name, technicalName, category, multiValued, slug);
        this.field = field;
    }

    public AttributeDefDto(UUID id, String name, String technicalName, FieldDto field) {
        super(id, name, technicalName);
        this.field = field;
    }

    public FieldDto getField() {
        return field;
    }

    public void setField(FieldDto field) {
        this.field = field;
    }

}
