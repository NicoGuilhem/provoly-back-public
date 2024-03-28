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
import com.speedment.jpastreamer.application.JPAStreamer;

@ApplicationScoped
public class CustomClassService {
    @PersistenceContext
    protected EntityManager em;
    private ObjectMapper objectMapper;
    private JPAStreamer jpaStreamer;

    public CustomClassService(ObjectMapper objectMapper, JPAStreamer jpaStreamer) {
        this.objectMapper = objectMapper;
        this.jpaStreamer = jpaStreamer;
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
        return jpaStreamer.stream(CustomClass.class).toList();
    }

    public List<CustomClass> getAllCustomClassByDomain(String domain) {
        return jpaStreamer.stream(CustomClass.class)
                .filter(CustomClass$.domain.equal(domain))
                .toList();
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
        return jpaStreamer.stream(CustomClass.class)
                .filter(CustomClass$.oClass.equal(oClass))
                .filter(CustomClass$.domain.equal(domain))
                .toList();
    }

    @Transactional
    public void deleteByoClassId(UUID oClassId) {
        var customClassesToDelete = findByoClassId(oClassId);
        if (!findByoClassId(oClassId).isEmpty()) {
            customClassesToDelete.forEach(customClass -> em.remove(em.merge(customClass)));
        }
    }

    public List<CustomClass> findByoClassId(UUID oClassId) {
        return jpaStreamer.stream(CustomClass.class)
                .filter(CustomClass$.oClass.equal(oClassId))
                .toList();
    }
}