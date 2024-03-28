package com.provoly.ref.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.provoly.common.Default;
import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntitySlug;

@Entity
public class Field extends EntitySlug {
    @Enumerated(EnumType.STRING)
    private Type type;

    private String crs;

    protected Field() {
        super();
    }

    @Default
    public Field(UUID id) {
        super(id);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }
}
