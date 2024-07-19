package com.provoly.ref.datasetversion;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.imports.MessageLevel;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.model.OClass;
import com.provoly.ref.model.OClass_;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasetVersionRepository {

    private static EntityIdRepository entityIdRepository;
    private Logger logger;
    public static final int PREVIEW_MAX_RESULT = 5;

    public DatasetVersionRepository(EntityIdRepository entityIdRepository, Logger logger) {
        this.entityIdRepository = entityIdRepository;
        this.logger = logger;
    }

    public Integer getNextVersion(UUID datasetId) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);

        q.where(cb.and(cb.equal(rootQuery.get(DatasetVersion_.dataset).get(Dataset_.id), datasetId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));

        var query = entityIdRepository.getEm().createQuery(q).setMaxResults(1);
        try {
            return query.getSingleResult().getVersion() + 1;
        } catch (NoResultException e) {
            logger.infof("No previous dataset version found for the dataset: %s, initializing first version.", datasetId);
            return 1;
        }
    }

    public void save(DatasetVersion datasetVersion) {
        entityIdRepository.saveEntity(datasetVersion);
    }

    public DatasetVersion getById(UUID id) {
        return entityIdRepository.getById(id, DatasetVersion.class);
    }

    public DatasetVersion findById(UUID id) {
        return entityIdRepository.findById(id, DatasetVersion.class);
    }

    @Transactional
    public List<DatasetVersion> getAll(DatasetVersionGetAllParams params) {
        EntityManager em = entityIdRepository.getEm();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        q.select(rootQuery);
        q.where(getFiltersAndAddFetchForGetAllParams(params,
                rootQuery, q, cb));

        var typedQuery = em.createQuery(q);
        params.addPaginationOptions(typedQuery);

        return typedQuery.getResultList();
    }

    public static Predicate getFiltersAndAddFetchForGetAllParams(DatasetVersionGetAllParams params,
            Root<DatasetVersion> rootQuery, CriteriaQuery<?> q, CriteriaBuilder cb) {
        //TODO this method should not and fetches and change query by reference. Refactor this method to only retrieve Predicate for filters
        Fetch<DatasetVersion, Dataset> fetch = rootQuery.fetch(DatasetVersion_.dataset);
        Fetch<Dataset, OClass> fetchOClass = fetch.fetch(Dataset_.oClass);
        fetchOClass.fetch(OClass_.attributes);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.orderBy(params.getOrderBy(cb, rootQuery, dataset));
        return params.getFilters(cb, rootQuery);
    }

    @Transactional
    public long getCountAll(DatasetVersionGetAllParams params) {
        EntityManager em = entityIdRepository.getEm();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(DatasetVersion.class);
        q.select(cb.count(rootQuery));
        q.where(params.getFilters(cb, rootQuery));
        TypedQuery<Long> query = em.createQuery(q);
        return query.getSingleResult();
    }

    public boolean exists(DatasetVersion entity) {
        return entityIdRepository.exists(entity);
    }

    public DatasetVersion getByName(String datasetName) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE),
                cb.equal(dataset.get(Dataset_.name), datasetName)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdRepository.getEm().createQuery(q).setMaxResults(1).getResultList().stream()
                .findFirst()
                .orElseThrow(() -> new ProvolyNotFoundException(
                        "No dataset version available for dataset '%s'".formatted(datasetName)));
    }

    public Collection<DatasetVersion> getAllActiveForClass(UUID classId) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE),
                cb.equal(dataset.get(Dataset_.oClass).get(OClass_.id), classId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdRepository.getEm().createQuery(q).getResultList()
                .stream()
                .collect(Collectors.groupingBy(dv -> dv.getDataset().getId()))
                .values()
                .stream()
                .map(values -> values.get(0))
                .filter(Objects::nonNull)
                .toList();
    }

    public Collection<DatasetVersion> getAllByDatasetId(UUID datasetId) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.and(
                cb.equal(dataset.get(Dataset_.id), datasetId)));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.VERSION)));
        return entityIdRepository.getEm().createQuery(q).getResultList();
    }

    public Optional<DatasetVersion> getByDatasetId(UUID datasetId) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        var dataset = rootQuery.join(DatasetVersion_.dataset);
        q.where(cb.equal(dataset.get(Dataset_.id), datasetId),
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.version)));
        return entityIdRepository.getEm().createQuery(q).setMaxResults(1).getResultList().stream().findFirst();
    }

    public List<DatasetVersion> getActiveVersionsOrderedByVersionNumberDesc(Collection<Dataset> datasetList) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersion.class);
        var rootQuery = q.from(DatasetVersion.class);
        Fetch<DatasetVersion, Dataset> fetch = rootQuery.fetch(DatasetVersion_.dataset);
        Fetch<Dataset, OClass> fetchOClass = fetch.fetch(Dataset_.oClass);
        fetchOClass.fetch(OClass_.attributes);
        q.where(rootQuery.get(DatasetVersion_.dataset).in(datasetList),
                cb.equal(rootQuery.get(DatasetVersion_.state), DatasetState.ACTIVE));
        q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.version)));
        return entityIdRepository.getEm().createQuery(q).getResultList();
    }

    public void deleteDatasetVersion(UUID datasetVersionId) {
        entityIdRepository.removeEntity(datasetVersionId, DatasetVersion.class);
    }

    public Long countLoadOrIndexingDatasetVersionByDataset(UUID datasetId) {
        CriteriaBuilder criteriaBuilder = entityIdRepository.getEm().getCriteriaBuilder();
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
        return entityIdRepository.getEm().createQuery(query).getSingleResult();
    }

    public void checkExists(UUID datasetVersionId) {
        entityIdRepository.checkEntityExists(datasetVersionId, DatasetVersion.class);
    }

    public List<DatasetVersionMessage> getDatasetVersionMessagesWithLevel(UUID datasetVersionId, MessageLevel messageLevel) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(DatasetVersionMessage.class);
        var rootQuery = q.from(DatasetVersionMessage.class);
        q.where(
                cb.and(
                        cb.equal(rootQuery.get(DatasetVersionMessage_.datasetVersionId), datasetVersionId),
                        cb.equal(rootQuery.get(DatasetVersionMessage_.level), messageLevel)));
        return entityIdRepository.getEm().createQuery(q).setMaxResults(PREVIEW_MAX_RESULT).getResultList();
    }

    public Long countDatasetVersionMessagesByLevel(UUID datasetVersionUuid, MessageLevel level) {
        var cb = entityIdRepository.getEm().getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(DatasetVersionMessage.class);
        q.select(cb.count(rootQuery));
        q.where(
                cb.and(
                        cb.equal(rootQuery.get(DatasetVersionMessage_.datasetVersionId), datasetVersionUuid),
                        cb.equal(rootQuery.get(DatasetVersionMessage_.level), level)));
        return entityIdRepository.getEm().createQuery(q).getSingleResult();
    }

    public DatasetVersion getLastVersionCreated(UUID datasetId) {
        entityIdRepository.checkEntityExists(datasetId, Dataset.class);
        try {
            var cb = entityIdRepository.getEm().getCriteriaBuilder();
            var q = cb.createQuery(DatasetVersion.class);
            var rootQuery = q.from(DatasetVersion.class);
            q.where(cb.equal(rootQuery.get(DatasetVersion_.dataset).get(Dataset_.id), datasetId));
            q.orderBy(cb.desc(rootQuery.get(DatasetVersion_.version)));
            return entityIdRepository.getEm().createQuery(q).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            throw new ProvolyNotFoundException("No dataset version found in dataset %s".formatted(datasetId));
        }
    }

}
