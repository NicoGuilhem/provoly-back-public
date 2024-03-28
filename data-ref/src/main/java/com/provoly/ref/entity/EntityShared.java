package com.provoly.ref.entity;

import java.util.UUID;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

import com.provoly.ref.user.VisibilityType;

@MappedSuperclass
public abstract class EntityShared extends EntityNamed {
    @Enumerated(EnumType.STRING)
    protected VisibilityType visibilityType;

    protected EntityShared() {
    }

    protected EntityShared(UUID id) {
        super(id);
    }

    protected EntityShared(UUID id, String name) {
        super(id, name);
    }

    public VisibilityType getVisibilityType() {
        return visibilityType;
    }

    public void setVisibilityType(VisibilityType visibilityType) {
        this.visibilityType = visibilityType;
    }

    protected boolean isPublic() {
        return visibilityType == VisibilityType.PUBLIC;
    }
}
