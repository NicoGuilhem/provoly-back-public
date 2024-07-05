package com.provoly.common.ref;

import com.provoly.common.model.field.FieldDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RefChangeEventFieldAdded extends RefChangeEvent {

    private final FieldDto field;

    @JsonCreator
    public RefChangeEventFieldAdded(FieldDto field) {
        super(Type.FIELD_ADDED);
        this.field = field;
    }

    public FieldDto getField() {
        return field;
    }
}
