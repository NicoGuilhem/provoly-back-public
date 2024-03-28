package com.provoly.ref.model;

import java.util.UUID;

import jakarta.persistence.Entity;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;

@Entity
public class Category extends EntityNamed {
    protected Category() {
    }

    @Default
    public Category(UUID id) {
        super(id);
    }

}
