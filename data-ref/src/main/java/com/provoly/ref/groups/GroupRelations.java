package com.provoly.ref.groups;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityId;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entityType")
public abstract class GroupRelations extends EntityId {
    @Column(insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    protected WithGroupEntityType entityType;

    protected UUID groupId;

    protected UUID entityId;

    @Default
    @JsonCreator
    protected GroupRelations(UUID id, WithGroupEntityType entityType, UUID groupId, UUID entityId) {
        super(id);
        this.entityType = entityType;
        this.groupId = groupId;
        this.entityId = entityId;
    }

    protected GroupRelations() {
    }

    public WithGroupEntityType getEntityType() {
        return entityType;
    }

    public UUID getGroupId() {
        return groupId;
    }

    protected UUID getEntityId() {
        return entityId;
    }
}
