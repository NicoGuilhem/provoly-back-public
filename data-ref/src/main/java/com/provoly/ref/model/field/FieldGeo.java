package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.model.Type;

@Entity
@DiscriminatorValue("GEO")
public class FieldGeo extends Field {
    private String crs;

    public FieldGeo(UUID id, String name, String slug, String crs, Type type) {
        super(id, name, slug, type);
        this.crs = crs;
    }

    protected FieldGeo() {

    }

    public String getCrs() {
        return crs;
    }
}
