package com.provoly.ref.relation;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
public class RelationTypeStatsService {

    private EntityIdRepository entityIdRepository;

    RelationTypeStatsService(EntityIdRepository entityIdRepository) {
        this.entityIdRepository = entityIdRepository;
    }

    @Transactional
    public RelationTypeStats getById(UUID id) {
        return entityIdRepository.getById(id, RelationTypeStats.class);
    }

    @Transactional
    public RelationTypeStats findById(UUID id) {
        return entityIdRepository.findById(id, RelationTypeStats.class);
    }

    @Transactional
    public List<RelationTypeStats> getAllRelationTypeStats() {
        return entityIdRepository.getAll(RelationTypeStats.class);
    }
}
