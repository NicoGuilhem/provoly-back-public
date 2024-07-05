package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;
import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("NUMERIC")
public class FieldNumeric extends Field {
    private boolean isLocaleFormat;
    private String unit;

    @JsonCreator
    @Default
    public FieldNumeric(UUID id, String name, String slug, Type type, boolean isLocaleFormat, String unit) {
        super(id, name, slug, type);
        this.isLocaleFormat = isLocaleFormat;
        this.unit = unit;
    }

    protected FieldNumeric() {

    }

    public boolean isLocaleFormat() {
        return isLocaleFormat;
    }

    public String getUnit() {
        return unit;
    }
}
