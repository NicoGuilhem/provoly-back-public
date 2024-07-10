package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.model.Type;

@Entity
@DiscriminatorValue("NUMERIC")
public class FieldNumeric extends Field {

    @Column(name = "is_locale_format")
    private boolean localeFormat;
    private String unit;

    public FieldNumeric(UUID id, String name, String slug, Type type, boolean localeFormat, String unit) {
        super(id, name, slug, type);
        this.localeFormat = localeFormat;
        this.unit = unit;
    }

    protected FieldNumeric() {

    }

    public boolean isLocaleFormat() {
        return localeFormat;
    }

    public void setLocaleFormat(boolean localeFormat) {
        this.localeFormat = localeFormat;
    }

    public String getUnit() {
        return unit;
    }
}
