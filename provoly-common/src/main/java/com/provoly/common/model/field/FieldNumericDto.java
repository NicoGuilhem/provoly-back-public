package com.provoly.common.model.field;

import java.util.UUID;

public class FieldNumericDto extends FieldDto {
    private boolean isLocaleFormat;
    private String unit;

    public FieldNumericDto(UUID id, String name, String type, String slug, boolean isLocaleFormat, String unit) {
        super(id, name, type, slug);
        this.isLocaleFormat = isLocaleFormat;
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isLocaleFormat() {
        return isLocaleFormat;
    }
}
