package com.provoly.ref.entity;

import java.util.UUID;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class EntitySlug extends EntityNamed {
    protected String slug;

    protected EntitySlug() {
        super();
    }

    protected EntitySlug(UUID id) {
        super(id);
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
