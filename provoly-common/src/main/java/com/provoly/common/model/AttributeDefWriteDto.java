package com.provoly.common.model;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AttributeDefWriteDto extends AttributeBaseDto {
    private UUID field;

    @Default
    @JsonCreator
    public AttributeDefWriteDto(UUID id, String name, String technicalName, UUID field, UUID category, boolean multiValued,
            String slug) {
        super(id, name, technicalName, category, multiValued, slug);
        this.field = field;
    }

    public AttributeDefWriteDto(UUID id, String name, String technicalName, UUID field) {
        super(id, name, technicalName);
        this.field = field;
    }

    public UUID getField() {
        return field;
    }

    public void setField(UUID field) {
        this.field = field;
    }

}
