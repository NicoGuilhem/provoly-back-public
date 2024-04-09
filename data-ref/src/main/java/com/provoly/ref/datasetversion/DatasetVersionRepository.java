package com.provoly.ref.datasetversion;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.imports.MessageLevel;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.model.OClass_;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasetVersionRepository {

    private EntityIdService entityIdService;
    private Logger logger;
    public static final int PREVIEW_MAX_RESULT = 5;

    private DatasetVersionRepository(EntityIdService entityIdService, Logger logger) {
        this.entityIdService = entityIdService;
        this.logger = logger;
    }

    public Integer getNextVersion(UUID datasetId) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);

        q.where(cb.and(cb.equal(rootQuery.get(DatasetVersion_.dataset).get(Dataset_.ID), datasetId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));

        var query = entityIdService.getEm().createQuery(q).setMaxResults(1);
        try {
            return query.getSingleResult().getVersion() + 1;
        } catch (NoResultException e) {
            logger.infof("No previous dataset version found for the dataset: %s, initializing first version.", datasetId);
            return 1;
        }
    }

    public void save(DatasetVersion datasetVersion) {
        entityIdService.saveEntity(datasetVersion);
    }

    public DatasetVersion getById(UUID id) {
        return entityIdService.getById(id, DatasetVersion.class);
    }

    public DatasetVersion findById(UUID id) {
        return entityIdService.findById(id, DatasetVersion.class);
    }

    public List<DatasetVersion> getAll() {
        return entityIdService.getAll(DatasetVersion.class);
    }

    public boolean exists(DatasetVersion entity) {
        return entityIdService.exists(entity);
    }

    public DatasetVersion getByName(String datasetName) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE),
                cb.equal(dataset.get(Dataset_.name), datasetName)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdService.getEm().createQuery(q).setMaxResults(1).getResultList().stream()
                .findFirst()
                .orElseThrow(() -> new ProvolyNotFoundException(
                        "No dataset version available for dataset '%s'".formatted(datasetName)));
    }

    public Collection<DatasetVersion> getAllActiveForClass(UUID classId) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE),
                cb.equal(dataset.get(Dataset_.oClass).get(OClass_.id), classId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdService.getEm().createQuery(q).getResultList()
                .stream()
                .collect(Collectors.groupingBy(dv -> dv.getDataset().getId()))
                .values()
                .stream()
                .map(values -> values.get(0))
                .filter(Objects::nonNull)
                .toList();
    }

    public Collection<DatasetVersion> getAllByDatasetId(UUID datasetId) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(dataset.get(Dataset_.id), datasetId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdService.getEm().createQuery(q).getResultList();
    }

    public DatasetVersion getByDatasetId(UUID datasetId) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.equal(dataset.get(Dataset_.id), datasetId),
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.version)));
        return entityIdService.getEm().createQuery(q).setMaxResults(1).getResultList().stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "No dataset version found for dataset %s.".formatted(datasetId)));
    }

    public void deleteDatasetVersion(UUID datasetVersionId) {
        entityIdService.removeEntity(datasetVersionId, DatasetVersion.class);
    }

    public Long countLoadOrIndexingDatasetVersionByDataset(UUID datasetId) {
        CriteriaBuilder criteriaBuilder = entityIdService.getEm().getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<DatasetVersion> datasetVersionRoot = query.from(DatasetVersion.class);

        Join<DatasetVersion, Dataset> datasetJoin = datasetVersionRoot.join(DatasetVersion_.dataset);
        query.select(criteriaBuilder.count(datasetVersionRoot))
                .where(
                        criteriaBuilder.and(
                                criteriaBuilder.equal(datasetJoin.get(Dataset_.id), datasetId),
                                criteriaBuilder.in(datasetVersionRoot.get(DatasetVersion_.state))
                                        .value(DatasetState.LOADING)
                                        .value(DatasetState.INDEXING)));
        //GetSingleResult can throw error but isExist is already called
        return entityIdService.getEm().createQuery(query).getSingleResult();
    }

    public void checkExists(UUID datasetVersionId) {
        entityIdService.checkEntityExists(datasetVersionId, DatasetVersion.class);
    }

    public List<DatasetVersionMessage> getDatasetVersionMessagesWithLevel(UUID datasetVersionId, MessageLevel messageLevel) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersionMessage.class);
        var rootQuery = q.from(DatasetVersionMessage.class);
        q.where(
                cb.and(
                        cb.equal(rootQuery.get(DatasetVersionMessage_.datasetVersionId), datasetVersionId),
                        cb.equal(rootQuery.get(DatasetVersionMessage_.level), messageLevel)));
        return entityIdService.getEm().createQuery(q).setMaxResults(PREVIEW_MAX_RESULT).getResultList();
    }

    public Long countDatasetVersionMessagesByLevel(UUID datasetVersionUuid, MessageLevel level) {
        var cb = entityIdService.getEm().getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(DatasetVersionMessage.class);
        q.select(cb.count(rootQuery));
        q.where(
                cb.and(
                        cb.equal(rootQuery.get(DatasetVersionMessage_.datasetVersionId), datasetVersionUuid),
                        cb.equal(rootQuery.get(DatasetVersionMessage_.level), level)));
        return entityIdService.getEm().createQuery(q).getSingleResult();
    }

}
