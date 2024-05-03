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

    @OneToOne
    protected Group group;

    protected UUID entityId;

    protected boolean canWrite;

    @Default
    @JsonCreator
    protected GroupRelations(UUID id, WithGroupEntityType entityType, Group group, UUID entityId, boolean canWrite) {
        super(id);
        this.entityType = entityType;
        this.group = group;
        this.entityId = entityId;
        this.canWrite = canWrite;
    }

    protected GroupRelations() {
    }

    public WithGroupEntityType getEntityType() {
        return entityType;
    }

    public Group getGroup() {
        return group;
    }

    protected UUID getEntityId() {
        return entityId;
    }

    public boolean canWrite() {
        return canWrite;
    }
}
