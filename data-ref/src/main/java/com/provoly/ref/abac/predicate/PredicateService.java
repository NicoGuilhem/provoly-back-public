package com.provoly.ref.abac.predicate;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
public class PredicateService {

    private EntityIdRepository entityIdRepository;

    PredicateService(EntityIdRepository entityIdRepository) {
        this.entityIdRepository = entityIdRepository;
    }

    @Transactional
    public List<Predicate> getAllPredicates() {
        return entityIdRepository.getAll(Predicate.class);
    }

    @Transactional
    public void save(Predicate predicate) {
        entityIdRepository.saveEntity(predicate);
    }

    @Transactional
    public Predicate getPredicate(UUID id) {
        return entityIdRepository.findById(id, Predicate.class);
    }
}
