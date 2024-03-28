package com.provoly.ref.entity;

import java.util.UUID;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class EntityNamed extends EntityId {
    protected String name;

    protected EntityNamed() {
        super();
    }

    protected EntityNamed(UUID id) {
        super(id);
    }

    protected EntityNamed(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
