package com.provoly.sync;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.DataVirt;
import com.provoly.clients.DatasetService;
import com.provoly.clients.DatasetVersionService;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.ItemDto;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.vertx.core.json.JsonObject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Service {

    @Inject
    Logger log;

    @Inject
    @RestClient
    DataVirt dataVirt;

    @Inject
    @RestClient
    DatasetVersionService datasetVersionService;

    @Inject
    @RestClient
    DatasetService datasetService;

    Instant bulkStartTime = Instant.now();
    long nbObject = 0L;

    public void consume(KafkaRecord<String, JsonObject> record) {
        consume(Collections.singleton(record));
    }

    public void consume(Collection<KafkaRecord<String, JsonObject>> records) {

        var datasetVersions = new HashMap<UUID, DatasetVersionDto>();

        String datasetName = null;
        DatasetVersionDto datasetVersion = null;

        var items = new ArrayList<ItemDto>();
        for (var record : records) {
            var recordDatasetName = extractDataset(record.getHeaders());
            if (!recordDatasetName.equals(datasetName)) {
                datasetName = recordDatasetName;
                datasetVersion = datasetService.getDatasetVersionByDatasetName(datasetName);
                datasetVersions.put(datasetVersion.getId(), datasetVersion);
            }

            var jsonItem = record.getPayload();
            var id = "";
            id = getId(record.getHeaders(), jsonItem);

            var ignoreFieldsHeader = extractIgnoreFields(record.getHeaders());

            var item = new ItemDto(datasetVersion, id);
            for (Map.Entry<String, Object> attribute : jsonItem) {
                if (!ignoreFieldsHeader.contains(attribute.getKey())) {
                    updateItemWithAttribute(item, attribute);
                }
            }

            items.add(item);

        }

        log.debug("Inserting into datastore");
        dataVirt.updateItems(items);

        // update older date into dataset
        log.debug("Updating dataset version");
        datasetVersions.values().forEach(ds -> {
            log.debugf("Updating last update for ds %s", ds.getId());
            ds = new DatasetVersionDto(ds.getId(), ds.getDataset(), ds.getoClass(), Instant.now(),
                    ds.getVersion(),
                    ds.getState(), ds.isWithFile());
            datasetVersionService.updateState(ds);
        });

        log.debugf("Mesuring");
        nbObject += records.size();
        var current = Instant.now();
        var bulkDuration = Duration.between(bulkStartTime, current);
        if (Duration.ofSeconds(10).minus(bulkDuration).isNegative()) { // Time to log
            double nbPerSecond = ((float) nbObject) / bulkDuration.getSeconds();
            log.infof("Write %d object(s) at %.2f objects/sec", nbObject, nbPerSecond);
            nbObject = 0;
            bulkStartTime = current;
        }

        log.debug("done");

    }

    private String extractDataset(Headers headers) {
        Header datasetDefinitionHeader = headers.lastHeader(HeaderName.FIELD_DATASET_DEFINITION);
        if (datasetDefinitionHeader == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Dataset header is mandatory");
        }
        return new String(datasetDefinitionHeader.value(), StandardCharsets.UTF_8);
    }

    private Set<String> extractIgnoreFields(Headers headers) {
        var fieldsIgnoreHeader = headers.lastHeader(HeaderName.FIELD_IGNORE);
        if (fieldsIgnoreHeader == null) {
            return Collections.emptySet();
        }

        String fieldsIgnoreAsString = new String(fieldsIgnoreHeader.value(), StandardCharsets.UTF_8);
        return Arrays.stream(fieldsIgnoreAsString.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private String getId(Headers header, JsonObject jsonItem) {
        var headerItemFieldsKey = header.lastHeader(HeaderName.FIELD_KEY);
        if (headerItemFieldsKey == null) {
            // Key is generated
            return UUID.randomUUID().toString();
        } else {
            // Key is build from data value
            String fieldsKeyNameAsString = new String(headerItemFieldsKey.value(), StandardCharsets.UTF_8);
            var fieldsKeyName = fieldsKeyNameAsString.split(",");
            StringBuilder id = new StringBuilder();
            boolean isFirst = true;
            for (String fieldKeyName : fieldsKeyName) {
                var fieldKeyValue = jsonItem.getString(fieldKeyName.trim());
                if (fieldKeyValue == null) {
                    throw new BusinessException(ErrorCode.FIELD_KEY_NOT_FOUND,
                            "Field for key not found or null: " + fieldsKeyNameAsString);
                }
                if (!isFirst) {
                    id.append('_');
                }
                isFirst = false;
                id.append(fieldKeyValue);
            }

            return id.toString();
        }
    }

    private void updateItemWithAttribute(ItemDto item, Map.Entry<String, Object> attribute) {
        if (attribute.getKey().equals("_metadata")) {
            var metadatas = ((JsonObject) attribute.getValue()).getMap();
            for (Map.Entry<String, Object> metadataEntry : metadatas.entrySet()) {
                if (metadataEntry.getKey().equals("_attribute")) {
                    // metadata are attribute metadata
                    var metadataAttributeMap = (Map<String, Map<String, String>>) metadataEntry.getValue();
                    updateItemAttributeWithMetadataMap(item, metadataAttributeMap);
                } else {
                    // metadata are item metadata
                    item.putMetadata(metadataEntry.getKey(), metadataEntry.getValue());
                }
            }
        } else {
            item.put(attribute.getKey(), attribute.getValue());
        }
    }

    private void updateItemAttributeWithMetadataMap(ItemDto item, Map<String, Map<String, String>> metadataAttributeMap) {
        for (var metadataAttributeEntry : metadataAttributeMap.entrySet()) {
            var attributeName = metadataAttributeEntry.getKey();
            var attributeMetadataEntries = metadataAttributeEntry.getValue();
            attributeMetadataEntries.forEach((metadataName, metadataValue) -> item.getSimpleAttribute(attributeName).metadata
                    .put(metadataName, metadataValue));
        }
    }

}