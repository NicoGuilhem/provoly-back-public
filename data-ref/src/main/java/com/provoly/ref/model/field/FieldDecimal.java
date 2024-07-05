package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;
import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("DECIMAL")
public class FieldDecimal extends FieldNumeric {
    private Integer decimalPrecision;

    @JsonCreator
    @Default
    public FieldDecimal(UUID id, String name, String slug, Type type, Integer decimalPrecision, boolean isLocaleFormat,
            String unit) {
        super(id, name, slug, type, isLocaleFormat, unit);
        this.decimalPrecision = decimalPrecision;
    }

    protected FieldDecimal() {

    }

    public Integer getDecimalPrecision() {
        return decimalPrecision;
    }
}