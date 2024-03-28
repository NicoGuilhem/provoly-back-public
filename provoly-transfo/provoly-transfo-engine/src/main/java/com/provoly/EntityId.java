package com.provoly;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import com.provoly.common.Default;

// TODO : Remove duplication with data-ref

@MappedSuperclass
public class EntityId implements Serializable {
    @Id
    protected UUID id;

    protected EntityId() {
        // Only for JPA
    }

    @Default
    public EntityId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EntityId entityId = (EntityId) o;
        return id.equals(entityId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
