package com.provoly.ref.abac.predicate;

import java.util.UUID;

import jakarta.persistence.Entity;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityId;

@Entity
public class Predicate extends EntityId {
    private String name;
    private String value;

    protected Predicate() {
        super();
    }

    @Default
    public Predicate(UUID id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
