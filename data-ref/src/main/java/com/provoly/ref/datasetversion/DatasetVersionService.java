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
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.imports.MessageLevel;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.model.OClass_;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasetVersionService {
    public static final int PREVIEW_MAX_RESULT = 5;
    private RefEventService eventService;
    private EntityIdService entityIdService;
    private DatasetVersionMessageService datasetVersionMessageService;
    private DatasetVersionMapper datasetVersionMapper;
    private Logger logger;

    public DatasetVersionService(RefEventService eventService,
            EntityIdService entityIdService,
            DatasetVersionMessageService datasetVersionMessageService,
            DatasetVersionMapper datasetVersionMapper, Logger logger) {
        this.eventService = eventService;
        this.entityIdService = entityIdService;
        this.datasetVersionMessageService = datasetVersionMessageService;
        this.datasetVersionMapper = datasetVersionMapper;
        this.logger = logger;
    }

    private Integer getNextVersion(UUID datasetId) {
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

    public void createDatasetVersion(DatasetVersion datasetVersion) {
        if (exists(datasetVersion)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Dataset version %s already exist."
                    .formatted(datasetVersion.getId()));
        }
        if (datasetVersion.getDataset().getType() == DatasetType.CLOSED) {
            datasetVersion.setState(datasetVersion.isWithFile() ? DatasetState.LOADING : DatasetState.INDEXING);

            checkCanCreate(datasetVersion);

            datasetVersion.setVersion(getNextVersion(datasetVersion.getDataset().getId()));

        } else {
            if (datasetVersion.isWithFile()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Dataset version %s is open, it cannot be associated to a source file."
                                .formatted(datasetVersion.getId()));
            }
            if (!getAllByDatasetId(datasetVersion.getDataset().getId()).isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Dataset %s can't have more than one version."
                        .formatted(datasetVersion.getDataset().getId()));
            }
            datasetVersion.setState(DatasetState.ACTIVE);
        }
        entityIdService.saveEntity(datasetVersion);
    }

    @Transactional
    public void updateState(DatasetVersion datasetVersion) {
        logger.infof("Set dataset version %s to state %s", datasetVersion.getId(), datasetVersion.getState());
        DatasetVersion oldDatasetVersion = getById(datasetVersion.getId());
        if (!oldDatasetVersion.getDataset().getId().equals(datasetVersion.getDataset().getId())) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE, "Dataset versions are immutable.");
        }
        setDatasetVersionState(oldDatasetVersion, datasetVersion.getState());
    }

    private void setDatasetVersionState(DatasetVersion datasetVersion, DatasetState wantedState) {
        verifyStateModificationIsAllowed(datasetVersion, wantedState);
        datasetVersion.setState(wantedState);
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

    @Transactional
    public void activateDatasetVersion(UUID datasetVersionId) {
        var datasetVersion = getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, DatasetState.ACTIVE);
        eventService.datasetActivated(datasetVersion);
    }

    @Transactional
    public void deactivateDatasetVersion(UUID datasetVersionId) {
        var datasetVersion = getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, DatasetState.INACTIVE);
    }

    @Transactional
    public void changeStateDatasetVersion(UUID datasetVersionId, DatasetState datasetState) {
        var datasetVersion = getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, datasetState);
    }

    public DatasetVersion deleteDatasetVersion(UUID datasetVersionId) {
        logger.infof("Starting to delete dataset version : %s", datasetVersionId);
        DatasetVersion datasetVersion = entityIdService.getById(datasetVersionId, DatasetVersion.class);

        if (!canDelete(datasetVersion.getState())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unable to delete dataset version %s because it's %s"
                    .formatted(datasetVersion.getId(), datasetVersion.getState()));
        }
        setDatasetVersionState(datasetVersion, DatasetState.DELETING);
        eventService.datasetVersionDeleted(datasetVersion);
        return datasetVersion;
    }

    public void deleteDatasetVersionAfterDeletingItems(UUID datasetVersionId) {
        logger.debug("Deleting dataset version %s".formatted(datasetVersionId));
        datasetVersionMessageService.deleteAllDatasetVersionMessage(datasetVersionId);
        entityIdService.removeEntity(datasetVersionId, DatasetVersion.class);
    }

    private boolean canDelete(DatasetState datasetState) {
        return datasetState != DatasetState.LOADING && datasetState != DatasetState.INDEXING
                && datasetState != DatasetState.DELETING;
    }

    private void checkCanCreate(DatasetVersion datasetVersion) {
        UUID datasetId = datasetVersion.getDataset().getId();

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
        var result = entityIdService.getEm().createQuery(query).getSingleResult();
        if (result > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Can't override existing dataset %s or have parallel imports.".formatted(datasetVersion.getId()));
        }
    }

    private void verifyStateModificationIsAllowed(DatasetVersion datasetVersion, DatasetState wantedState) {
        DatasetState currentState = datasetVersion.getState();
        if (currentState == wantedState) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Dataset %s is already in state: %s".formatted(datasetVersion.getId(), wantedState));
        }

        if (!currentState.canUpdateTo(wantedState)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Dataset cannot transition from %s to %s".formatted(currentState, wantedState));
        }
    }

    @Transactional
    public List<MessageDto> getDatasetVersionPreviewsDto(UUID datasetVersionId) {
        entityIdService.checkEntityExists(datasetVersionId, DatasetVersion.class);
        List<DatasetVersionMessage> datasetVersionMessagesError = getDatasetVersionMessagesWithLevel(datasetVersionId,
                MessageLevel.ERROR);
        List<DatasetVersionMessage> datasetVersionMessagesWarning = getDatasetVersionMessagesWithLevel(datasetVersionId,
                MessageLevel.WARNING);

        if (datasetVersionMessagesError.isEmpty() && datasetVersionMessagesWarning.isEmpty()) {
            return List.of();
        }
        Long errorCount = countDatasetVersionMessagesByLevel(datasetVersionId, MessageLevel.ERROR);
        Long warningCount = countDatasetVersionMessagesByLevel(datasetVersionId, MessageLevel.WARNING);

        MessageDto messageErrors = new MessageDto(MessageLevel.ERROR,
                datasetVersionMapper.toCollectionDto(datasetVersionMessagesError), errorCount);
        MessageDto messageWarnings = new MessageDto(MessageLevel.WARNING,
                datasetVersionMapper.toCollectionDto(datasetVersionMessagesWarning),
                warningCount);

        return List.of(messageErrors, messageWarnings);
    }

    private List<DatasetVersionMessage> getDatasetVersionMessagesWithLevel(UUID datasetVersionId, MessageLevel messageLevel) {
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
