package com.provoly.ref.category;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
public class Category extends EntityNamed {
    @Enumerated(EnumType.STRING)
    private WithCategoryEntityType withCategoryEntityType;

    @Default
    @JsonCreator
    public Category(UUID id, String name, WithCategoryEntityType withCategoryEntityType) {
        super(id, name);
        this.withCategoryEntityType = withCategoryEntityType;
    }

    protected Category() {
    }

    public WithCategoryEntityType getWithCategoryEntityType() {
        return withCategoryEntityType;
    }

    public void setWithCategoryEntityType(WithCategoryEntityType withCategoryEntityType) {
        this.withCategoryEntityType = withCategoryEntityType;
    }
}
