package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.model.Type;

@Entity
@DiscriminatorValue("DECIMAL")
public class FieldDecimal extends FieldNumeric {
    private Integer decimalPrecision;

    public FieldDecimal(UUID id, String name, String slug, Type type, Integer decimalPrecision, boolean localeFormat,
            String unit) {
        super(id, name, slug, type, localeFormat, unit);
        this.decimalPrecision = decimalPrecision;
    }

    protected FieldDecimal() {

    }

    public Integer getDecimalPrecision() {
        return decimalPrecision;
    }
}