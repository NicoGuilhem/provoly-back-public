package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.model.Type;

@Entity
@DiscriminatorValue("DATE")
public class FieldDate extends Field {
    private String format;

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
