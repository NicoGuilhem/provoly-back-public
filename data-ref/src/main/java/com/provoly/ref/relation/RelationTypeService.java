package com.provoly.ref.relation;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
public class RelationTypeService {

    private RelationTypeMapper mapper;
    private EntityIdService entityIdService;
    private EntityManager em;

    RelationTypeService(RelationTypeMapper mapper, EntityManager em, EntityIdService entityIdService) {
        this.mapper = mapper;
        this.em = em;
        this.entityIdService = entityIdService;
    }

    @Transactional
    public void saveOrUpdate(RelationTypeDto relationTypeDto) {
        checkNameAlreadyExists(relationTypeDto);
        if (relationTypeDto.name.length() > 30) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Max length is 30");
        }
        var relationType = entityIdService.findById(relationTypeDto.id, RelationType.class);
        if (relationType == null) {
            entityIdService.saveEntity(mapper.toModel(relationTypeDto));
        } else {
            mapper.update(relationTypeDto, relationType);
        }
    }

    @Transactional
    public RelationType getById(UUID id) {
        return entityIdService.getById(id, RelationType.class);
    }

    @Transactional
    public Collection<RelationType> getAll() {
        return entityIdService.getAll(RelationType.class);
    }

    @Transactional
    public void delete(UUID id) {
        entityIdService.removeEntity(id, RelationType.class);
    }

    private void checkNameAlreadyExists(RelationTypeDto relationTypeDto) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(RelationType.class);
        var root = q.from(RelationType.class);

        Predicate sameNamePredicate = cb.equal(root.get(RelationType_.name), relationTypeDto.name);
        q.where(sameNamePredicate);

        var relationTypes = em.createQuery(q).getResultList();

        if (!relationTypes.isEmpty()) {
            var existingRelationTypes = relationTypes.get(0);
            if (!existingRelationTypes.getId().equals(relationTypeDto.id)) { // it's not an update of same
                String message = MessageFormat.format("A relation already exists with same name: {0}",
                        existingRelationTypes.getName());
                throw new BusinessException(ErrorCode.NAME_ALREADY_USED, message);
            }
        }
    }

}
