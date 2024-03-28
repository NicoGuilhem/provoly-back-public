package com.provoly.ref.metadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
public class MetadataDefService {
    private EntityManager em;
    private EntityIdService entityIdService;

    public MetadataDefService(EntityManager em, EntityIdService entityIdService) {
        this.em = em;
        this.entityIdService = entityIdService;
    }

    @Transactional
    public void addMetadata(MetadataDef metadata) {
        var values = metadata.getValues();

        if (!values.isEmpty() && metadata.getType() != VariableType.LIST) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only type LIST accept allowedValues");
        } // a bouger dans metadatadef
        if (metadata.getType() == VariableType.LIST && values.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Metadata type LIST must have allowedValues");
        }

        entityIdService.saveEntity(metadata);
    }

    @Transactional
    public MetadataDef getById(UUID metadataId) {
        return entityIdService.getById(metadataId, MetadataDef.class);
    }

    @Transactional
    public List<MetadataDef> getAllMetadataDef() {
        return entityIdService.getAll(MetadataDef.class);
    }

    //only used in test
    @Transactional
    public void delete(UUID id) {
        entityIdService.removeIfExists(id, MetadataDef.class);
    }

    @Transactional
    public Optional<? extends MetadataDef> getByName(String metaName, Class<? extends MetadataDef> metadataDefClass) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(metadataDefClass);
        var metadataRoot = q.from(metadataDefClass);
        q.where(cb.equal(metadataRoot.get(MetadataDef_.NAME), metaName));
        return em.createQuery(q).getResultList().stream().findFirst();
    }

    @Transactional
    public Optional<? extends MetadataDef> getBySlug(String metaSlug, Class<? extends MetadataDef> metadataDefClass) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(metadataDefClass);
        var metadataRoot = q.from(metadataDefClass);
        q.where(cb.equal(metadataRoot.get(MetadataDef_.SLUG), metaSlug));
        return em.createQuery(q).getResultList().stream().findFirst();
    }

}
