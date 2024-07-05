package com.provoly.common.model.field;

import java.util.UUID;

public class FieldDecimalDto extends FieldDto {
    private Integer decimalPrecision;
    private boolean isLocaleFormat;
    private String unit;

    public FieldDecimalDto(UUID id, String name, String slug, String type, Integer decimalPrecision, boolean isLocaleFormat,
            String unit) {
        super(id, name, type, slug);
        this.decimalPrecision = decimalPrecision;
        this.isLocaleFormat = isLocaleFormat;
        this.unit = unit;
    }

    public Integer getDecimalPrecision() {
        return decimalPrecision;
    }

    public boolean isLocaleFormat() {
        return isLocaleFormat;
    }

    public String getUnit() {
        return unit;
    }
}
