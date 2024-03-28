package com.provoly.ref.relation;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
public class RelationTypeStatsService {

    private EntityIdService entityIdService;

    RelationTypeStatsService(EntityIdService entityIdService) {
        this.entityIdService = entityIdService;
    }

    @Transactional
    public RelationTypeStats getById(UUID id) {
        return entityIdService.getById(id, RelationTypeStats.class);
    }

    @Transactional
    public RelationTypeStats findById(UUID id) {
        return entityIdService.findById(id, RelationTypeStats.class);
    }

    @Transactional
    public List<RelationTypeStats> getAllRelationTypeStats() {
        return entityIdService.getAll(RelationTypeStats.class);
    }
}
