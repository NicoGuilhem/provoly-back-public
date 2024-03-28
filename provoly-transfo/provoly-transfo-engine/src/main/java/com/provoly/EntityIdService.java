package com.provoly;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;

@ApplicationScoped
public class EntityIdService {

    @PersistenceContext
    protected EntityManager em;

    //    @Transactional
    //    public void saveEntity(EntityId entity) {
    //        if (entity.getId() == null) {
    //            throw new BusinessException(ErrorCode.TECHNICAL, "Id is required");
    //        }
    //        if (findById(entity.getId(), entity.getClass()) != null) {
    //            em.merge(entity);
    //        } else {
    //            if (entity instanceof EntityNamed) {
    //                isNameAlreadyExists(((EntityNamed) entity).getName(), ((EntityNamed) entity).getClass());
    //            }
    //            em.persist(entity);
    //        }
    //    }

    @Transactional
    public void persist(EntityId entity) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Id is required");
        }
        em.persist(entity);
    }

    @Transactional
    public void merge(EntityId entity) {
        em.merge(entity);
    }

    @Transactional
    public <T extends EntityId> void removeEntity(UUID id, Class<T> entityClass) {
        em.remove(getById(id, entityClass));
    }

    //    @Transactional
    //    public <T extends EntityId> void removeIfExists(UUID id, Class<T> entityClass) {
    //        T entity = findById(id, entityClass);
    //        if (entity != null) {
    //            removeEntity(entity);
    //        }
    //    }

    @Transactional
    public void removeEntity(EntityId entityId) {
        em.remove(entityId);
    }

    public <T extends EntityId> List<T> getAll(Class<T> entityClass) {
        var q = em.getCriteriaBuilder().createQuery(entityClass);
        q.select(q.from(entityClass));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public <T extends EntityId> Optional<T> findById(UUID id, Class<T> entityClass) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Transactional
    public <T extends EntityId> T getById(UUID id, Class<T> entityClass) throws BusinessException {
        return findById(id, entityClass).orElseThrow(
                () -> new ProvolyNotFoundException(entityClass, id));
    }

    //    @Transactional
    //    public <T extends EntityNamed> void isNameAlreadyExists(String name, Class<T> entityClass) {
    //        var cb = em.getCriteriaBuilder();
    //        var q = cb.createQuery(entityClass);
    //        var metadataRoot = q.from(entityClass);
    //        q.where(cb.equal(metadataRoot.get(EntityNamed_.NAME), name));
    //        var result = em.createQuery(q).getResultList();
    //        if (result.size() != 0) {
    //            throw new BusinessException(ErrorCode.NAME_ALREADY_USED, "Name %s already exists".formatted(name));
    //        }
    //    }

    @Transactional
    public <T extends EntityId> boolean checkEntityExists(UUID id, Class<T> entityClass) {
        return em.find(entityClass, id) != null;
    }

}
