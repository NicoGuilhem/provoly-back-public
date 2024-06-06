package com.provoly.ref.category;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityId;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
public class CategoryRelations extends EntityId {
    @OneToOne
    protected Category category;

    protected UUID entityId;

    @Default
    @JsonCreator
    public CategoryRelations(UUID id, Category category, UUID entityId) {
        super(id);
        this.category = category;
        this.entityId = entityId;
    }

    protected CategoryRelations() {
    }

    public Category getCategory() {
        return category;
    }
}
