package com.provoly.common.model.field;

import java.util.UUID;

public class FieldNumericDto extends FieldDto {
    private boolean localeFormat;
    private String unit;

    public FieldNumericDto(UUID id, String name, String type, String slug, boolean localeFormat, String unit) {
        super(id, name, type, slug);
        this.localeFormat = localeFormat;
        this.unit = unit;
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
