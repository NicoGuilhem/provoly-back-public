package com.provoly.ref.customclass;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.model.OClass;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class CustomClassService {
    @PersistenceContext
    protected EntityManager em;
    private ObjectMapper objectMapper;

    public CustomClassService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addCustomClass(UUID id, String domain, String content) {
        try {
            // validate if content is a JSON
            objectMapper.readTree(content);
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Json content is invalid");
        }

        var customClass = new CustomClass(id, domain, content);
        save(customClass);
    }

    public CustomClass getCustomClass(UUID oClass, String domain) {
        var result = findByOClassAndDomain(oClass, domain);
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    CustomClass.class.getSimpleName() + " avec id=[" + oClass + "] et domain=[" + domain + "] inexistant.");
        } else {
            return result.get(0);
        }
    }

    @Transactional
    public List<CustomClass> getAllCustomClass() {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomClass.class);
        return em.createQuery(q).getResultList();
    }

    public List<CustomClass> getAllCustomClassByDomain(String domain) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomClass.class);
        var root = q.from(CustomClass.class);
        q = q.where(
                cb.equal(root.get(CustomClass_.domain), domain));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public void save(CustomClass customClass) {
        if (em.find(OClass.class, customClass.getoClass()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "oClass with id " + customClass.getoClass() + " not exists");
        }
        if (!findByOClassAndDomain(customClass.getoClass(), customClass.getDomain()).isEmpty()) {
            em.merge(customClass);
        } else {
            em.persist(customClass);
        }
    }

    @Transactional
    public List<CustomClass> findByOClassAndDomain(UUID oClass, String domain) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomClass.class);
        var root = q.from(CustomClass.class);
        q = q.where(cb.and(
                cb.equal(root.get(CustomClass_.oClass), oClass)),
                cb.equal(root.get(CustomClass_.domain), domain));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public void deleteByoClassId(UUID oClassId) {
        var customClassesToDelete = findByoClassId(oClassId);
        if (!findByoClassId(oClassId).isEmpty()) {
            customClassesToDelete.forEach(customClass -> em.remove(em.merge(customClass)));
        }
    }

    public List<CustomClass> findByoClassId(UUID oClassId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomClass.class);
        var root = q.from(CustomClass.class);
        q = q.where(
                cb.equal(root.get(CustomClass_.oClass), oClassId));
        return em.createQuery(q).getResultList();
    }
}