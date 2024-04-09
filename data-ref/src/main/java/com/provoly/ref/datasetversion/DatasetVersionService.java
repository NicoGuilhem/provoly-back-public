package com.provoly.ref.datasetversion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.imports.MessageLevel;
import com.provoly.ref.event.RefEventService;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasetVersionService {

    private RefEventService eventService;
    private DatasetVersionMessageService datasetVersionMessageService;
    private DatasetVersionMapper datasetVersionMapper;
    private Logger logger;
    private DatasetVersionRepository datasetVersionRepository;

    public DatasetVersionService(RefEventService eventService,
            DatasetVersionMessageService datasetVersionMessageService,
            DatasetVersionMapper datasetVersionMapper, Logger logger, DatasetVersionRepository datasetVersionRepository) {
        this.eventService = eventService;
        this.datasetVersionMessageService = datasetVersionMessageService;
        this.datasetVersionMapper = datasetVersionMapper;
        this.logger = logger;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    public void createDatasetVersion(DatasetVersion datasetVersion) {
        if (datasetVersionRepository.exists(datasetVersion)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Dataset version %s already exist."
                    .formatted(datasetVersion.getId()));
        }
        if (datasetVersion.getDataset().getType() == DatasetType.CLOSED) {
            datasetVersion.setState(datasetVersion.isWithFile() ? DatasetState.LOADING : DatasetState.INDEXING);

            checkCanCreateClosedDatasetVersion(datasetVersion);

            datasetVersion.setVersion(datasetVersionRepository.getNextVersion(datasetVersion.getDataset().getId()));

        } else {
            if (datasetVersion.isWithFile()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Dataset version %s is open, it cannot be associated to a source file."
                                .formatted(datasetVersion.getId()));
            }
            if (!datasetVersionRepository.getAllByDatasetId(datasetVersion.getDataset().getId()).isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Dataset %s can't have more than one version."
                        .formatted(datasetVersion.getDataset().getId()));
            }
            datasetVersion.setState(DatasetState.ACTIVE);
        }
        datasetVersionRepository.save(datasetVersion);
    }

    @Transactional
    public void updateState(DatasetVersion datasetVersion) {
        logger.infof("Set dataset version %s to state %s", datasetVersion.getId(), datasetVersion.getState());
        DatasetVersion oldDatasetVersion = datasetVersionRepository.getById(datasetVersion.getId());
        if (!oldDatasetVersion.getDataset().getId().equals(datasetVersion.getDataset().getId())) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE, "Dataset versions are immutable.");
        }
        setDatasetVersionState(oldDatasetVersion, datasetVersion.getState());
    }

    private void setDatasetVersionState(DatasetVersion datasetVersion, DatasetState wantedState) {
        verifyStateModificationIsAllowed(datasetVersion, wantedState);
        datasetVersion.setState(wantedState);
    }

    @Transactional
    public void activateDatasetVersion(UUID datasetVersionId) {
        var datasetVersion = datasetVersionRepository.getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, DatasetState.ACTIVE);
        eventService.datasetActivated(datasetVersion);
    }

    @Transactional
    public void deactivateDatasetVersion(UUID datasetVersionId) {
        var datasetVersion = datasetVersionRepository.getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, DatasetState.INACTIVE);
    }

    @Transactional
    public void changeStateDatasetVersion(UUID datasetVersionId, DatasetState datasetState) {
        var datasetVersion = datasetVersionRepository.getById(datasetVersionId);
        setDatasetVersionState(datasetVersion, datasetState);
    }

    public DatasetVersion deleteDatasetVersion(UUID datasetVersionId) {
        logger.infof("Starting to delete dataset version : %s", datasetVersionId);
        DatasetVersion datasetVersion = datasetVersionRepository.getById(datasetVersionId);

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
        datasetVersionRepository.deleteDatasetVersion(datasetVersionId);
    }

    private boolean canDelete(DatasetState datasetState) {
        return datasetState != DatasetState.LOADING && datasetState != DatasetState.INDEXING
                && datasetState != DatasetState.DELETING;
    }

    private void checkCanCreateClosedDatasetVersion(DatasetVersion datasetVersion) {
        UUID datasetId = datasetVersion.getDataset().getId();

        if (datasetVersion.getProducer() == null || datasetVersion.getProducer().isBlank()
                || datasetVersion.getProductionDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Producer and Production date are mandatory for closed dataset");
        }

        if (datasetVersion.getProductionDate().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Production date cannot be in the future");
        }

        if (datasetVersionRepository.countLoadOrIndexingDatasetVersionByDataset(datasetId) > 0) {
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
        datasetVersionRepository.checkExists(datasetVersionId);
        List<DatasetVersionMessage> datasetVersionMessagesError = datasetVersionRepository.getDatasetVersionMessagesWithLevel(
                datasetVersionId,
                MessageLevel.ERROR);
        List<DatasetVersionMessage> datasetVersionMessagesWarning = datasetVersionRepository.getDatasetVersionMessagesWithLevel(
                datasetVersionId,
                MessageLevel.WARNING);

        if (datasetVersionMessagesError.isEmpty() && datasetVersionMessagesWarning.isEmpty()) {
            return List.of();
        }
        Long errorCount = datasetVersionRepository.countDatasetVersionMessagesByLevel(datasetVersionId, MessageLevel.ERROR);
        Long warningCount = datasetVersionRepository.countDatasetVersionMessagesByLevel(datasetVersionId, MessageLevel.WARNING);

        MessageDto messageErrors = new MessageDto(MessageLevel.ERROR,
                datasetVersionMapper.toCollectionDto(datasetVersionMessagesError), errorCount);
        MessageDto messageWarnings = new MessageDto(MessageLevel.WARNING,
                datasetVersionMapper.toCollectionDto(datasetVersionMessagesWarning),
                warningCount);

        return List.of(messageErrors, messageWarnings);
    }

}
