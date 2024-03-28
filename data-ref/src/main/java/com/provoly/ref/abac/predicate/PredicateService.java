package com.provoly.ref.abac.predicate;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
public class PredicateService {

    private EntityIdService entityIdService;

    PredicateService(EntityIdService entityIdService) {
        this.entityIdService = entityIdService;
    }

    @Transactional
    public List<Predicate> getAllPredicates() {
        return entityIdService.getAll(Predicate.class);
    }

    @Transactional
    public void save(Predicate predicate) {
        entityIdService.saveEntity(predicate);
    }

    @Transactional
    public Predicate getPredicate(UUID id) {
        return entityIdService.findById(id, Predicate.class);
    }
}
