package com.provoly.virt.imports;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetService;
import com.provoly.clients.DatasetVersionService;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.ExtractedMessage;
import com.provoly.common.imports.ImportsMessage;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.item.ItemDto;
import com.provoly.virt.ProvolySpanManager;
import com.provoly.virt.dataset.DatasetController;
import com.provoly.virt.dataset.ImportRequest;
import com.provoly.virt.entity.FileType;
import com.provoly.virt.event.VirtEventEmitter;
import com.provoly.virt.file.FileService;

import io.opentelemetry.api.trace.Span;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.eventbus.EventBus;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ImportService {
    private FileService fileService;

    private DatasetVersionService datasetVersionService;

    private DatasetService datasetService;

    private ImportRunner importRunner;

    private ProvolySpanManager spanManager;

    private VirtEventEmitter virtEventEmitter;
    private final Logger log;

    private final EventBus bus;

    public ImportService(Logger log, FileService fileService, @RestClient DatasetVersionService datasetVersionService,
            @RestClient DatasetService datasetService, ImportRunner importRunner, ProvolySpanManager tracer,
            VirtEventEmitter virtEventEmitter, EventBus bus) {
        this.fileService = fileService;
        this.datasetVersionService = datasetVersionService;
        this.datasetService = datasetService;
        this.importRunner = importRunner;
        this.spanManager = tracer;
        this.log = log;
        this.virtEventEmitter = virtEventEmitter;
        this.bus = bus;
    }

    public DatasetVersionDto runImportFromFile(UUID datasetId, Path file, FileType mediaType, boolean normalizeGeo,
            Integer chunkSize) {
        log.infof("Importing new dataset for datasetId %s", datasetId);
        if (file == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is required.");
        }

        DatasetDto datasetDto = datasetService.get(datasetId);
        UUID datasetVersionId = UUID.randomUUID();

        DatasetVersionDto dto = new DatasetVersionDto(datasetVersionId, datasetId, datasetDto.getoClass(),
                DatasetState.LOADING, true);

        datasetVersionService.create(dto);
        Span span = spanManager.generateSpan("Minio upload",
                Map.of("fileName", file.getFileName().toString()));

        log.infof("Uploading file to raw storage for datasetId %s", datasetId);
        try (InputStream is = new FileInputStream(file.toFile())) {
            fileService.associateFileToDataset(is, mediaType, dto);
        } catch (Exception e) {
            spanManager.recordException(span, e);

            virtEventEmitter.sendDatasetVersion(new DatasetVersionDto(dto.getId(), dto.getDataset(), DatasetState.ERROR));
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Error while saving file %s, can't create dataset version %s for dataset: %s due to : %s".formatted(
                            file.getFileName().toString(),
                            datasetVersionId,
                            datasetId,
                            e.getMessage()),
                    e);
        } finally {
            span.end();
        }

        DatasetVersionDto datasetVersionDtoIndexing = new DatasetVersionDto(dto.getId(), dto.getDataset(),
                DatasetState.INDEXING);
        datasetVersionService.updateState(datasetVersionDtoIndexing);
        log.infof("Starting indexing %s", datasetId);

        bus.publish("importFromFile", new ImportEvent(dto, normalizeGeo, chunkSize));

        return datasetVersionService.get(dto.getId());
    }

    public DatasetVersionDto runImportFromItems(UUID datasetId, UUID id, Collection<ItemDto> items) {
        log.infof("Asking dataset#%s import", id);

        DatasetDto dataset = datasetService.get(datasetId);
        if (dataset == null) {
            throw new ProvolyNotFoundException(DatasetController.class, datasetId);
        }
        if (DatasetType.CLOSED != dataset.getType()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Import is only allowed on closed dataset.");
        }
        DatasetVersionDto datasetVersionDto = new DatasetVersionDto(id, datasetId, dataset.getoClass(), DatasetState.INDEXING);
        datasetVersionService.create(datasetVersionDto);

        bus.publish("importFromItems", new ImportRequest(dataset, datasetVersionDto, items));
        return datasetVersionDto;
    }

    /**
     * To get asynchoneous process AND mdc propagation
     *
     * @param event
     */
    @ConsumeEvent(value = "importFromFile")
    @Blocking
    public void importFromFile(ImportEvent event) {
        Infrastructure.getDefaultWorkerPool().submit(() -> {
            var isTerminatedWithoutError = importRunner.importItemsFromFile(event.dto().getId(), event.normalizeGeo(),
                    event.chunkSize());
            updateVersionState(event.dto(), isTerminatedWithoutError);
        });
    }

    /**
     * To get asynchoneous process AND mdc propagation
     *
     * @param importRequest
     */
    @ConsumeEvent(value = "importFromItems")
    @Blocking
    public void importFromItems(ImportRequest importRequest) {
        Infrastructure.getDefaultWorkerPool().submit(() -> {
            try {
                importRunner.importItemsFromItemDto(importRequest);
                log.infof("Dataset#%s imported", importRequest.dataset().getId());

            } catch (RuntimeException e) {
                handleError(e, importRequest.datasetVersion());
            }
        });
    }

    public void handleError(Throwable t, DatasetVersionDto datasetVersion) {
        log.errorf(t, "An error occurred while injecting items for dataset version %s", datasetVersion.getId());
        DatasetVersionDto errorDatasetVersionDto = new DatasetVersionDto(datasetVersion.getId(), datasetVersion.getDataset(),
                DatasetState.ERROR);
        virtEventEmitter.sendDatasetVersionAndImportMessage(errorDatasetVersionDto,
                new ImportsMessage(
                        datasetVersion.getId(),
                        null,
                        List.of(new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.FORMAT))));
    }

    private void updateVersionState(DatasetVersionDto dto, boolean isTerminatedWithoutErrors) {
        if (isTerminatedWithoutErrors) {
            log.debugf("update dataset version state to ACTIVE");
            virtEventEmitter.sendDatasetVersion(new DatasetVersionDto(dto.getId(), dto.getDataset(), DatasetState.ACTIVE));
        } else {
            log.debugf("update dataset version state to ERROR");
            virtEventEmitter.sendDatasetVersion(new DatasetVersionDto(dto.getId(), dto.getDataset(), DatasetState.ERROR));
        }
    }

    public record ImportEvent(DatasetVersionDto dto, Boolean normalizeGeo, Integer chunkSize) {
    }
}