package com.provoly.ref.relation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntitySlug;

@Entity
public class RelationType extends EntitySlug {
    private Instant modificationDate;

    @OneToOne(cascade = CascadeType.ALL)
    private RelationTypeStats relationTypeStats;

    protected RelationType() {
        super();
    }

    @Default
    public RelationType(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public Instant getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Instant modificationDate) {
        this.modificationDate = modificationDate;
    }

    public RelationTypeStats getRelationTypeStats() {
        return relationTypeStats;
    }

    public void setRelationTypeStats(RelationTypeStats relationTypeStats) {
        this.relationTypeStats = relationTypeStats;
    }
}
