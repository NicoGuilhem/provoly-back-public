package com.provoly.ref.metadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.WithMetadata;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.model.OClass;

import org.jboss.logging.Logger;

@ApplicationScoped
public class MetadataService {

    private EntityManager em;
    private EntityIdRepository entityIdRepository;
    private MetadataDefService metadataDefService;
    private DatasetVersionRepository datasetVersionRepository;
    private Logger log;

    public MetadataService(EntityManager em, EntityIdRepository entityIdRepository, MetadataDefService metadataDefService,
            DatasetVersionRepository datasetVersionRepository,
            Logger log) {
        this.em = em;
        this.entityIdRepository = entityIdRepository;
        this.metadataDefService = metadataDefService;
        this.datasetVersionRepository = datasetVersionRepository;
        this.log = log;
    }

    private Optional<MetadataValue> getMetadataValueAssignedToEntity(UUID entityId, UUID metadataDefId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(MetadataValue.class);
        var rootQuery = q.from(MetadataValue.class);
        q.where(cb.and(cb.equal(rootQuery.get(MetadataValue_.entityId), entityId),
                cb.equal(rootQuery.get(MetadataValue_.metadataDefId), metadataDefId)));
        return em.createQuery(q).getResultStream().findFirst();
    }

    private void checkEntityExist(EntityType entityType, UUID entityId) {
        switch (entityType) {
            case DASHBOARD -> entityIdRepository.checkEntityExists(entityId, Dashboard.class);
            case CLASS -> entityIdRepository.checkEntityExists(entityId, OClass.class);
            case DATASET -> entityIdRepository.checkEntityExists(entityId, Dataset.class);
            case DATASET_VERSION -> datasetVersionRepository.checkExists(entityId);
        }
    }

    @Transactional
    public void deleteMetadataValueByEntityId(UUID entityId, UUID metadataDefId, EntityType entityType) {
        checkEntityExist(entityType, entityId);
        entityIdRepository.checkEntityExists(metadataDefId, MetadataDef.class);
        var metadataValue = getMetadataValueAssignedToEntity(entityId, metadataDefId);
        metadataValue.ifPresentOrElse(
                mv -> em.remove(mv),
                () -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Metadata %s is not assigned to %s %s".formatted(metadataDefId,
                                    entityType.name().toLowerCase(),
                                    entityId));
                });
    }

    public List<MetadataValue> getMetadataValueByEntityId(UUID entityId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(MetadataValue.class);
        var metadataRoot = q.from(MetadataValue.class);
        q.select(metadataRoot).where(cb.equal(metadataRoot.get(MetadataValue_.entityId), entityId));
        return em.createQuery(q).getResultList();
    }

    public <T extends WithMetadata> void updateMetadataByEntityType(T entityWriteDto, EntityType entityType) {
        if (entityWriteDto.getMetadata() == null) {
            log.debugf("No metadata provided for %s entity %s", entityType.name(), entityWriteDto.getId());
            return;
        }

        if (!entityWriteDto.getMetadata().isEmpty()) {
            checkMetadataIsUpdatable(entityWriteDto.getMetadata());
        }
        var metadatas = getMetadataValueByEntityId(entityWriteDto.getId());
        metadatas.forEach(
                metadata -> deleteMetadataValueByEntityId(entityWriteDto.getId(), metadata.getMetadataDefId(),
                        entityType));
        if (!entityWriteDto.getMetadata().isEmpty()) {
            entityWriteDto.getMetadata().forEach(
                    metadata -> addMetadataToEntity(entityWriteDto.getId(), metadata.getMetadataDefId(), metadata,
                            entityType));
        }
    }

    @Transactional
    public void addMetadataToEntity(UUID entityId, UUID metadataDefId,
            MetadataValueWriteDto metadataValueWriteDto, EntityType entityType) {
        checkEntityExist(entityType, entityId);
        var metadatadef = metadataDefService.getById(metadataDefId);

        var metadataValue = getMetadataValueAssignedToEntity(entityId, metadatadef.getId());
        metadataValue.ifPresentOrElse(
                mv -> mv.validateAndSetValue(metadataValueWriteDto.getValue(), metadatadef.getType(), metadatadef.getValues()),
                () -> {
                    var newMetadataValue = new MetadataValue(entityType, entityId, metadatadef.getId());
                    newMetadataValue.validateAndSetValue(metadataValueWriteDto.getValue(), metadatadef.getType(),
                            metadatadef.getValues());
                    entityIdRepository.saveEntity(newMetadataValue, false);
                });
    }

    private void checkMetadataIsUpdatable(List<MetadataValueWriteDto> metadata) {
        metadata.forEach(metadataValueWriteDto -> {
            MetadataDef metadataDef = entityIdRepository.getById(metadataValueWriteDto.getMetadataDefId(), MetadataDef.class);
            if (metadataDef.isSystem() && metadataDef.isReadOnly()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "MetadataDef %s is not updatable.".formatted(metadataValueWriteDto.getMetadataDefId()));
            }
        });
    }
}
