package com.provoly.ref.entity;

import java.util.List;
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

    public EntityManager getEm() {
        return em;
    }

    @Transactional
    public void saveEntity(EntityId entity) {
        saveEntity(entity, true);
    }

    @Transactional
    public void saveEntity(EntityId entity, boolean checkNameDuplicate) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Id is required");
        }
        if (findById(entity.getId(), entity.getClass()) != null) {
            em.merge(entity);
        } else {
            if (checkNameDuplicate && entity instanceof EntityNamed named) {
                checkNameAlreadyExists(named.getName(), named.getClass());
            }
            em.persist(entity);
        }
    }

    @Transactional
    public <T extends EntityId> void removeEntity(UUID id, Class<T> entityClass) {
        em.remove(getById(id, entityClass));
    }

    @Transactional
    public <T extends EntityId> void removeIfExists(UUID id, Class<T> entityClass) {
        T entity = findById(id, entityClass);
        if (entity != null) {
            removeEntity(entity);
        }
    }

    @Transactional
    public void removeEntity(EntityId entityId) {
        em.remove(entityId);
    }

    @Transactional
    public <T extends EntityId> List<T> getAll(Class<T> entityClass) {
        var q = em.getCriteriaBuilder().createQuery(entityClass);
        q.select(q.from(entityClass));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public <T extends EntityId> T getById(UUID id, Class<T> entityClass) throws ProvolyNotFoundException {
        T entity = findById(id, entityClass);
        if (entity == null) {
            throw new ProvolyNotFoundException(entityClass, id);
        }
        return entity;
    }

    @Transactional
    public <T extends EntityId> T findById(UUID id, Class<T> entityClass) {
        return em.find(entityClass, id);
    }

    @Transactional
    public <T extends EntityId> T getLinkedById(UUID id, Class<T> entityClass) throws BusinessException {
        T entity = findById(id, entityClass);
        if (entity == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "%s : %s inexistant.".formatted(entityClass.getSimpleName(), id));
        }
        return entity;
    }

    @Transactional
    public <T extends EntityNamed> void checkNameAlreadyExists(String name, Class<T> entityClass) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var metadataRoot = q.from(entityClass);
        q.select(cb.count(metadataRoot));
        q.where(cb.equal(metadataRoot.get(EntityNamed_.NAME), name));
        var result = em.createQuery(q).getSingleResult();
        if (result > 0) {
            throw new BusinessException(ErrorCode.NAME_ALREADY_USED, "Name %s already exists".formatted(name));
        }
    }

    @Transactional
    public <T extends EntityId> boolean exists(T entity) {
        return em.find(entity.getClass(), entity.getId()) != null;
    }

    @Transactional
    public <T extends EntityId> void checkEntityExists(UUID id, Class<T> entityClass) {
        if (em.find(entityClass, id) == null) {
            throw new ProvolyNotFoundException(entityClass, id);
        }
    }
}
