package com.provoly.virt.imports;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetVersionService;
import com.provoly.clients.ModelService;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.imports.*;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.dataset.ImportRequest;
import com.provoly.virt.event.VirtEventEmitter;
import com.provoly.virt.file.FileService;
import com.provoly.virt.imports.indexers.FileWalker;
import com.provoly.virt.imports.model.ConversionResult;
import com.provoly.virt.imports.model.ItemRecord;
import com.provoly.virt.item.WriteItemsService;
import com.provoly.virt.storage.InsertionError;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ImportRunner {

    private ModelService modelService;

    private DatasetVersionService datasetVersionService;

    private FileDispatcher fileDispatcher;

    private WriteItemsService writeItemsService;

    private FileService fileService;

    private RecordConvertor recordConvertor;

    private Logger log;
    private DataVirtProperties dataVirtProperties;
    private VirtEventEmitter virtEventEmitter;

    public ImportRunner(@RestClient OidcModelService modelService,
            @RestClient DatasetVersionService datasetVersionService,
            FileDispatcher fileDispatcher,
            WriteItemsService writeItemsService,
            FileService fileService,
            RecordConvertor recordConvertor,
            Logger log,
            DataVirtProperties dataVirtProperties,
            VirtEventEmitter virtEventEmitter) {
        this.modelService = modelService;
        this.datasetVersionService = datasetVersionService;
        this.fileDispatcher = fileDispatcher;
        this.writeItemsService = writeItemsService;
        this.fileService = fileService;
        this.recordConvertor = recordConvertor;
        this.log = log;
        this.dataVirtProperties = dataVirtProperties;
        this.virtEventEmitter = virtEventEmitter;
    }

    @WithSpan
    public boolean importItemsFromFile(@SpanAttribute(value = "datasetVersionId") UUID datasetVersionId,
            @SpanAttribute(value = "normalizeGeo") boolean normalizeGeo,
            @SpanAttribute(value = "chunkSize") int chunkSize) {
        long errorsCount = 0;
        long itemsCount = 0;

        log.info("Get dataset %s and associated OClass".formatted(datasetVersionId));
        DatasetVersionDto datasetVersionDto = datasetVersionService.get(datasetVersionId);
        OClassDetailsDto oClassDto = modelService.getDetails(datasetVersionDto.getoClass());
        List<String> oClassAttributes = oClassDto.getAttributes().stream().map(attribute -> attribute.technicalName).toList();
        log.infof("Get dataset associated file: %s", datasetVersionId);

        try (FileWalker fileWalker = fileDispatcher.dispatch(fileService.getFile(datasetVersionDto.getId().toString()),
                oClassAttributes)) {
            log.info("Validate file attributes");
            List<ExtractedMessage> headerMessages = recordConvertor.validateHeaders(fileWalker.getAttributes(),
                    oClassAttributes);

            if (!headerMessages.isEmpty()) {
                log.warn("Problems detected while validating file headers.");
                virtEventEmitter.sendImportMessage(new ImportsMessage(datasetVersionId, null, headerMessages));

                if (messagesHaveAnyErrors(headerMessages)) {
                    log.error("Error while validating attributes, aborting process");
                    return false;
                }
            }

            List<ItemDto> items = new ArrayList<>();
            Map<String, String> recordIdToItemId = new HashMap<>();

            log.info("Starting process");

            while (fileWalker.hasNext()) {
                ItemRecord itemRecord = fileWalker.next();

                itemsCount++;
                log.tracef("Working with record : %s", itemRecord.recordId());

                if (items.size() >= chunkSize) {
                    log.debugf("Sending %s items to be saved", items.size());
                    errorsCount += saveChunk(items, recordIdToItemId, datasetVersionId);
                    items.clear();
                    recordIdToItemId.clear();
                    log.infof("A total of %s items has already been saved, with a total of %s errors", itemsCount, errorsCount);
                    log.debug("Starting new chunk");
                }

                ConversionResult convertedRecord = recordConvertor.convert(itemRecord, oClassDto, normalizeGeo);

                if (errorsCount == 0 && convertedRecord.messages().isEmpty()) {
                    ItemDto item = mapRecordToItem(convertedRecord.record(), datasetVersionDto);
                    items.add(item);
                    recordIdToItemId.put(item.getId(), itemRecord.recordId());

                } else {
                    if (!convertedRecord.messages().isEmpty()) {
                        virtEventEmitter.sendImportMessage(
                                new ImportsMessage(datasetVersionId, itemRecord.recordId(), convertedRecord.messages()));

                        errorsCount += convertedRecord.messages().size();
                        log.infof("Errors detected on record, adding to errors stack. %s registered", errorsCount);
                    }
                    if (errorsCount >= dataVirtProperties.importMaxErrors()) {
                        log.error("Errors size greater than max import error %s. Aborting process".formatted(
                                dataVirtProperties.importMaxErrors()));
                        return false;
                    }
                }
            }

            if (errorsCount == 0 && !items.isEmpty()) {
                log.infof("Saving last %s items", items.size());
                errorsCount += saveChunk(items, recordIdToItemId, datasetVersionId);
            }

        } catch (Error | Exception e) {
            log.errorf("An error as occured during import : %s", e);
            var message = e.getMessage() != null ? e.getMessage() : e.toString();
            ExtractedMessage error = new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.STORAGE,
                    new FileImportDto.ParamsTypeError(message));

            virtEventEmitter.sendImportMessage(new ImportsMessage(datasetVersionId, null, List.of(error)));
            return false;
        }

        log.info("Successful termination of process");
        return errorsCount == 0;
    }

    public boolean messagesHaveAnyErrors(List<ExtractedMessage> extractedMessages) {
        return extractedMessages
                .stream()
                .anyMatch(message -> message.messageLevel().equals(MessageLevel.ERROR));
    }

    public void importItemsFromItemDto(ImportRequest request) {
        log.infof("Starting dataset version#%s import", request);
        var datasetVersion = request.datasetVersion();

        if (!isAllItemIdsMatchDatasetVersionId(request.items(), datasetVersion)) {
            log.errorf("One or more Item ids don't match dataset id %s", datasetVersion.getId());
            sendDatasetVersionState(datasetVersion, DatasetState.ERROR);
            virtEventEmitter.sendImportMessage(new ImportsMessage(
                    datasetVersion.getId(),
                    null,
                    List.of(new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.FORMAT))));
            return;
        }
        List<InsertionError> errors = writeItemsService.addItemsDto(request.items());

        if (!errors.isEmpty()) {
            log.infof("There are errors %s during import %s", errors.size(), request);
            sendDatasetVersionState(datasetVersion, DatasetState.ERROR);
            virtEventEmitter.sendImportMessage(new ImportsMessage(
                    request.datasetVersion().getId(),
                    null,
                    errors.stream().map(this::mapToExtractedMessage).toList()));
            return;
        }

        sendDatasetVersionState(datasetVersion, DatasetState.ACTIVE);
        log.infof("Dataset version#%s imported with %d items", datasetVersion.getId(), request.items().size());
    }

    private void sendDatasetVersionState(DatasetVersionDto datasetVersion, DatasetState error) {
        var activeDatasetVersion = new DatasetVersionDto(
                datasetVersion.getId(),
                datasetVersion.getDataset(),
                datasetVersion.getoClass(),
                error);
        virtEventEmitter.sendDatasetVersion(activeDatasetVersion);
    }

    private boolean isAllItemIdsMatchDatasetVersionId(Collection<ItemDto> items, DatasetVersionDto datasetVersion) {
        return items.stream().allMatch(item -> item.getDatasetVersionId().equals(datasetVersion.getId().toString()));
    }

    private Long saveChunk(List<ItemDto> items, Map<String, String> recordIdToItemId, UUID datasetVersionId) {
        List<InsertionError> responses = writeItemsService.addItemsDto(items);
        return responses.stream()
                .filter(response -> response.error() != null)
                .map(storageError -> sendStorageError(recordIdToItemId, datasetVersionId, storageError))
                .count();
    }

    private int sendStorageError(Map<String, String> recordIdToItemId, UUID datasetVersionId, InsertionError storageError) {
        log.errorf("Errors on record %s : %s", recordIdToItemId.get(storageError.itemId()), storageError.error());
        virtEventEmitter.sendImportMessage(new ImportsMessage(
                datasetVersionId,
                recordIdToItemId.get(storageError.itemId()),
                List.of(mapToExtractedMessage(storageError))));
        return 1;
    }

    private ExtractedMessage mapToExtractedMessage(InsertionError elasticErrors) {
        return new ExtractedMessage(
                MessageLevel.ERROR,
                ExtractMessageCode.STORAGE,
                new FileImportDto.ParamsTypeError(elasticErrors.error()));
    }

    private ItemDto mapRecordToItem(ItemRecord itemRecord,
            DatasetVersionDto datasetVersionDto) {
        var itemDto = new ItemDto(datasetVersionDto, UUID.randomUUID().toString());

        itemRecord.values().entrySet().stream().filter(val -> val.getValue() != null)
                .forEach(val -> itemDto.put(val.getKey(), val.getValue()));

        return itemDto;
    }
}