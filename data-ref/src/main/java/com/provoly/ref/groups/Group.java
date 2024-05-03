package com.provoly.ref.groups;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@Table(name = "GroupDef")
public class Group extends EntityNamed {
    private boolean system = false;

    @Default
    @JsonCreator
    public Group(UUID id, String name, boolean system) {
        super(id, name);
        this.system = system;
    }

    public Group() {
    }

    public boolean isSystem() {
        return system;
    }
}
