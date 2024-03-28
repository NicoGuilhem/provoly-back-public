package com.provoly.ref.relation;

import java.util.UUID;

import jakarta.persistence.Entity;

import com.provoly.ref.entity.EntityId;

@Entity
public class RelationTypeStats extends EntityId {

    private int nbRelation;

    public RelationTypeStats() {
    }

    public RelationTypeStats(UUID id, int nbRelation) {
        super(id);
        this.nbRelation = nbRelation;
    }

    public int getNbRelation() {
        return nbRelation;
    }
}
