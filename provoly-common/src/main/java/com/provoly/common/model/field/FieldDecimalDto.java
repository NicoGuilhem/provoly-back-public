package com.provoly.common.model.field;

import java.util.UUID;

public class FieldDecimalDto extends FieldDto {
    private Integer decimalPrecision;
    private boolean localeFormat;
    private String unit;

    public FieldDecimalDto(UUID id, String name, String slug, String type, Integer decimalPrecision, boolean localeFormat,
            String unit) {
        super(id, name, type, slug);
        this.decimalPrecision = decimalPrecision;
        this.localeFormat = localeFormat;
        this.unit = unit;
    }

    public Integer getDecimalPrecision() {
        return decimalPrecision;
    }

    public boolean isLocaleFormat() {
        return localeFormat;
    }

    public String getUnit() {
        return unit;
    }
}
