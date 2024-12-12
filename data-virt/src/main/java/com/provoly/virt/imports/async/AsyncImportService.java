package com.provoly.virt.imports.async;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetService;
import com.provoly.clients.ModelService;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.virt.imports.RecordConvertor;
import com.provoly.virt.imports.model.ItemRecord;
import com.provoly.virt.item.WriteItemsService;
import com.provoly.virt.kafka.KafkaTools;

import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AsyncImportService {
    private static final String ITEM_ID = "provoly-item-id";
    private static final String DATASET_VERSION_ID = "provoly-dataset-version-id";
    private static final String ITEM_UPDATE_MODE = "provoly-item-update-mode";

    private Logger log;
    private WriteItemsService writeItemsService;
    private DatasetService datasetService;
    private ModelService modelService;
    private RecordConvertor recordConvertor;

    public AsyncImportService(Logger log, WriteItemsService writeItemsService,
            @RestClient DatasetService datasetService,
            @RestClient ModelService modelService,
            RecordConvertor recordConvertor) {
        this.log = log;
        this.writeItemsService = writeItemsService;
        this.datasetService = datasetService;
        this.modelService = modelService;
        this.recordConvertor = recordConvertor;
    }

    @Incoming("item")
    @Blocking
    public void consume(ConsumerRecord<String, JsonObject> recordMessage) {
        var items = new ArrayList<ItemDto>();
        var datasetVersionId = extractDatasetVersion(recordMessage);

        var dataset = datasetService.searchByDatasetVersionId(datasetVersionId);
        if (dataset.getType() == DatasetType.CLOSED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Dataset %s can't be closed".formatted(dataset.getId()));
        }
        var oClassDetails = modelService.getDetails(dataset.getoClass());

        var jsonItem = recordMessage.value();
        var id = getId(recordMessage);

        ItemRecord itemRecord = new ItemRecord(id, jsonItem.getMap());

        var messages = recordConvertor.validateAttributeNames(
                itemRecord.values().keySet(),
                oClassDetails.getAttributes().stream().map(AttributeDefDetailsDto::getTechnicalName))
                .stream()
                .filter(m -> m.messageLevel() == MessageLevel.ERROR)
                .toList();

        if (!messages.isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Error during attributes validation : %s".formatted(messages));
        }

        var convertResult = recordConvertor.convert(itemRecord, oClassDetails, false, GeoFormat.WKT);

        if (convertResult.messages().isEmpty()) {
            var item = buildItemDto(convertResult.record().values(), // FIXME we convert record into itemdto to be converted again... https://github.com/Provoly/provoly-back/issues/245
                    oClassDetails.getId(),
                    "%s@%s".formatted(datasetVersionId, id));
            items.add(item);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "values invalid : %s".formatted(convertResult.messages()));
        }

        writeItemsService.addOrUpdateItemsDto(items, extractUpdateMode(recordMessage));
        log.debugf("%s items converted and inserted", items.size());
    }

    private ItemDto buildItemDto(Map<String, Object> attributes, UUID oClass, String id) {
        var item = new ItemDto(oClass, id);
        for (var attribute : attributes.entrySet()) {
            if (attribute.getValue() != null) {
                item.put(attribute.getKey(), attribute.getValue());
            }
        }
        return item;
    }

    private UUID extractDatasetVersion(ConsumerRecord<String, JsonObject> message) {
        return UUID.fromString(KafkaTools.extractHeaderValueFromRecord(message, DATASET_VERSION_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        "Missing mandatory header %s".formatted(DATASET_VERSION_ID))));
    }

    private ItemUpdateMode extractUpdateMode(ConsumerRecord<String, JsonObject> message) {
        return KafkaTools.extractHeaderValueFromRecord(message, ITEM_UPDATE_MODE)
                .map(ItemUpdateMode::fromString)
                .orElse(ItemUpdateMode.REPLACE);
    }

    private String getId(ConsumerRecord<String, JsonObject> message) {
        return KafkaTools.extractHeaderValueFromRecord(message, ITEM_ID)
                .orElse(UUID.randomUUID().toString()); // if id is not provided, we generate a new one
    }

}