package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;
import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("DATE")
public class FieldDate extends Field {
    private String format;

    @JsonCreator
    @Default
    public FieldDate(UUID id, String name, String slug, Type type, String format) {
        super(id, name, slug, type);
        this.format = format;
    }

    protected FieldDate() {

    }

    public String getFormat() {
        return format;
    }
}
