package com.provoly.ref.metaProvisioning;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetaProvisioningDto;
import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
public class MetaProvisioningService {
    private MetaProvisioningMapper mapper;
    private EntityIdRepository entityIdRepository;
    private EntityManager em;

    MetaProvisioningService(MetaProvisioningMapper mapper, EntityManager em, EntityIdRepository entityIdRepository) {
        this.mapper = mapper;
        this.em = em;
        this.entityIdRepository = entityIdRepository;
    }

    @Transactional
    public void saveOrUpdate(MetaProvisioningDto metaProvisioningDto) {
        MetaProvisioning metaProvisioning = mapper.toModel(metaProvisioningDto);
        checkAlreadyLinkedWithMetadata(metaProvisioning);

        if (!metaProvisioning.getMetadata().getType().equals(metaProvisioning.getUserProfile().getType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Metadata and user profile must have same type");
        }

        var metaPro = findById(metaProvisioningDto.id());
        if (metaPro == null) {
            entityIdRepository.saveEntity(metaProvisioning);
        } else {
            mapper.update(metaProvisioningDto, metaPro);
        }
    }

    @Transactional
    public void checkAlreadyLinkedWithMetadata(MetaProvisioning meta) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(MetaProvisioning.class);
        var root = q.from(MetaProvisioning.class);
        Predicate sameMetadataUser = cb.equal(root.get(MetaProvisioning_.userProfile), meta.getUserProfile());
        Predicate sameMetadataItem = cb.equal(root.get(MetaProvisioning_.metadata), meta.getMetadata());

        q.select(root).where(cb.and(sameMetadataUser, sameMetadataItem));
        List<MetaProvisioning> res = em.createQuery(q).getResultList();

        if (checkIsSameEntity(meta, res)) {
            throw new BusinessException(ErrorCode.ID_ALREADY_USED,
                    "This user profile is already linked to this metadata item");
        }
    }

    private boolean checkIsSameEntity(MetaProvisioning meta, List<MetaProvisioning> res) {
        if (res.isEmpty()) {
            return false;
        }
        return res.get(0).getId().equals(meta.getId());
    }

    public List<MetaProvisioning> getAllMetaprovisionings() {
        return entityIdRepository.getAll(MetaProvisioning.class);
    }

    public MetaProvisioning findById(UUID id) {
        return entityIdRepository.findById(id, MetaProvisioning.class);
    }

    public MetaProvisioning getById(UUID id) {
        return entityIdRepository.getById(id, MetaProvisioning.class);
    }

    public void removeEntity(UUID id) {
        entityIdRepository.removeEntity(id, MetaProvisioning.class);
    }

    public void removeIfExists(UUID id) {
        entityIdRepository.removeIfExists(id, MetaProvisioning.class);
    }
}
