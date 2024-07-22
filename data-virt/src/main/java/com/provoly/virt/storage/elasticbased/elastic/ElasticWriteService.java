package com.provoly.virt.storage.elasticbased.elastic;

import static com.provoly.virt.storage.elasticbased.ElasticSupport.elasticGeoFormat;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.MetadataRefService;
import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.CountDto;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.*;
import com.provoly.virt.storage.InsertionError;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageWriteService;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.StorageLayout;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.elasticsearch.client.ResponseException;
import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@StorageQualifier(Storage.ELASTIC)
@ApplicationScoped
class ElasticWriteService implements StorageWriteService {
    private Logger log;

    private ElasticsearchClient elastic;

    private MetadataRefService metadataService;

    private ElasticLayout elasticLayout;

    private ElasticSupport elasticSupport;

    public ElasticWriteService(ElasticsearchClient elastic,
            @RestClient MetadataRefService metadataService,
            ElasticLayout elasticLayout,
            ElasticSupport elasticSupport,
            Logger log) {
        this.elastic = elastic;
        this.metadataService = metadataService;
        this.elasticLayout = elasticLayout;
        this.elasticSupport = elasticSupport;
        this.log = log;
    }

    /////////////////////////////////:
    // ITEM -> Elastic
    public List<InsertionError> add(Collection<Item> items) {
        return addItems(items, AtomicInteger::new);

    }

    private List<InsertionError> addItems(Collection<Item> items, IntConsumer chunkSizeCallBack) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        AtomicInteger chunkSize = new AtomicInteger(0);
        List<InsertionError> errors = new ArrayList<>();
        try {
            log.infof("called with %s items", items.size());
            BulkResponse response = bulkItems(items);
            log.debugf("  Took %d / ingest took %d", response.took(), response.ingestTook());
            chunkSizeCallBack.accept(response.items().size());
            if (response.errors()) {
                errors.addAll(response.items()
                        .stream()
                        .filter(item -> item.error() != null)
                        .map(item -> new InsertionError(item.id(),
                                item.error().causedBy() != null ? item.error().causedBy().reason() : item.error().reason()))
                        .toList());
            }
            return errors;
        } catch (ResponseException e) {
            int statusCode = e.getResponse().getStatusLine().getStatusCode();
            if (statusCode != 413 || items.size() == 1) {
                throw new IllegalStateException("Unable to store %s items".formatted(items.size()), e);
            }
            log.info("Collection too large, start splitting");

            List<Item> itemAsList = new ArrayList<>(items);

            List<Item> firstHalf = itemAsList.subList(0, itemAsList.size() / 2);
            errors.addAll(addItems(firstHalf, chunkSize::set));
            log.debug("First sublist inserted");

            chunkAndBulkRemainingItems(chunkSize, errors, itemAsList, firstHalf.size());
            chunkSizeCallBack.accept(chunkSize.get());
            return errors;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to store %s items".formatted(items.size()), e);
        }
    }

    private void chunkAndBulkRemainingItems(AtomicInteger chunkSize, List<InsertionError> errors, List<Item> itemAsList,
            int start) {
        boolean shouldContinue = true;
        while (shouldContinue) {
            int nextStop = Math.min(start + chunkSize.get(), itemAsList.size());
            log.debugf("Bulking from %s to %s", start, nextStop);
            List<Item> nextChunk = itemAsList.subList(start, nextStop);
            errors.addAll(addItems(nextChunk, chunkSize::set));
            if (nextChunk.size() < chunkSize.get()) {
                shouldContinue = false;
            } else {
                log.tracef("chunk size %s", chunkSize.get());
                start += chunkSize.get();
            }
        }
    }

    private BulkResponse bulkItems(Collection<Item> items) throws IOException {
        var request = new BulkRequest.Builder();
        for (Item item : items) {
            request.operations(b -> b.index(
                    i -> i.index(item.getoClass().getSlug())
                            .id(item.getIdAsString())
                            .document(buildSource(item))));
            log.tracef("Add request : indexName=[%s] id=[%s] item=[%s]", item.getoClass().getSlug(), item.getId(), item);
        }

        elasticLayout.prepareRequest(request);
        log.tracef("Adding %s item(s)", items.size());

        return elastic.bulk(request.build());
    }

    private Map<String, ?> buildSource(Item item) {
        var itemMap = new HashMap<String, Object>(); // Root map
        var itemMetadataMap = new HashMap<String, Object>(); // All item metadata - by metadata name
        itemMap.put(StorageLayout.META_FIELD_NAME, itemMetadataMap);
        var valuesMap = new HashMap<String, Object>(); // All attributes value - by attributes name
        itemMap.put(StorageLayout.ATTRIBUTE_FIELD_NAME, valuesMap);
        // Add all metadata to item
        for (MetadataValueDto metadataValueDto : item.getMetadata()) {
            var metadataName = elasticLayout.buildElasticMetadataName(metadataValueDto);
            itemMetadataMap.put(metadataName, metadataValueDto.getValue());
        }

        // Add all simple value attributes
        for (AttributeSimpleValue attribute : item.getAttributes(AttributeSimpleValue.class)) {
            var valueMap = new HashMap<String, Object>();
            valuesMap.put(StorageLayout.SIMPLE_ITEM_PREFIX + attribute.getSlug(), valueMap);
            addSimpleValue(attribute, valueMap);
        }

        // Add all multi value attributes
        for (AttributeMultiValue attributes : item.getAttributes(AttributeMultiValue.class)) {
            var multiValues = new ArrayList<>();
            valuesMap.put(StorageLayout.MULTI_ITEM_PREFIX + attributes.getSlug(), multiValues);

            // For every values in the attribute
            for (AttributeSimpleValue attribute : attributes.getValues()) {

                var valueMap = new HashMap<String, Object>();
                multiValues.add(valueMap);
                addSimpleValue(attribute, valueMap);
            }
        }

        return itemMap;
    }

    private void addSimpleValue(AttributeSimpleValue attribute, HashMap<String, Object> valueMap) {
        var valueMetaMap = new HashMap<String, Object>();
        valueMap.put(StorageLayout.META_FIELD_NAME, valueMetaMap);
        for (MetadataValueDto metadataValueDto : attribute.getMetadata()) {
            valueMetaMap.put(elasticLayout.buildElasticMetadataName(metadataValueDto), metadataValueDto.getValue());
        }

        valueMap.put(elasticLayout.buildElasticAttributeName(attribute), convertToElasticValue(attribute));
    }

    private Object convertToElasticValue(AttributeSimpleValue attribute) {
        var value = attribute.readValueEvenIfNotVisible();
        return switch (value) {
            case null -> null;
            case Instant i -> i.toString();
            case GeoHolder geo -> geo.getStringAs(elasticGeoFormat);
            default -> value;
        };
    }

    /////////////////////////////////:
    // Elastic -> ITEM

    public ItemsSearchResult convertToItemResult(SearchResponse<Map> response, OClassDetailsDto oClass, boolean isWithCount) {
        ItemsSearchResult result = new ItemsSearchResult();
        for (var hit : response.hits().hits()) {
            var item = convertToItem(hit, oClass);
            result.add(item);
            if (!hit.sort().isEmpty()) {
                var searchAfters = hit.sort()
                        .stream()
                        .map(fieldValue -> fieldValue._get().toString())
                        .toList();

                result.setSearchAfter(new SearchAfterContext(response.pitId(), searchAfters));
            }
        }

        if (response.hits().total() == null) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "elasticsearch response hit total is null, not possible to reach the count and the relation");
        }

        if (isWithCount) {
            long totalSize = response.hits().total().value();
            boolean isAccurate = response.hits().total().relation().name().equals("Eq");
            log.debugv("Total size {0} - is size accurate : {1}", totalSize, isAccurate);
            result.setCount(Map.of(oClass.getId(), new CountDto(totalSize, isAccurate)));
        }

        return result;
    }

    private Item convertToItem(Hit<Map> hit, OClassDetailsDto oClass) {
        var itemMap = hit.source();
        var itemMetaMap = (Map<String, Object>) itemMap.getOrDefault(StorageLayout.META_FIELD_NAME, new HashMap<>());
        var itemAttributeMap = (Map<String, Object>) itemMap.get(StorageLayout.ATTRIBUTE_FIELD_NAME);
        //UUID oClassId = UUID.fromString((String) provolyMeta.get("class")); // TODO : Check coherence
        Item item = new Item(new ItemId(hit.id()), oClass);

        // Load item metadata from hit
        for (Map.Entry<String, Object> metadata : itemMetaMap.entrySet()) {
            var metadataDef = extractMetadataDef(metadata);
            if (metadataDef != null) { // We ignoring unknown metadata
                item.add(new MetadataValueDto(metadataDef, metadata.getValue()));
            }
        }

        // Load attributes : Based on attributes defined in Class. Everything else is ignored
        for (AttributeDefDetailsDto attributeDef : oClass.getAttributes()) {
            if (attributeDef.isMultiValued()) {
                var attributes = (Iterable<Map<String, Object>>) itemAttributeMap
                        .get(StorageLayout.MULTI_ITEM_PREFIX + attributeDef.getSlug());
                if (attributes == null)
                    continue;
                var attributeMultiValue = item.getAttributeMulti(attributeDef.getTechnicalName());
                for (Map<String, Object> attribute : attributes) {
                    var attributeValue = attributeMultiValue.addValue();
                    extractAttributeValue(attributeValue, attribute);
                }

            } else {
                var attribute = (Map<String, Object>) itemAttributeMap
                        .get(StorageLayout.SIMPLE_ITEM_PREFIX + attributeDef.getSlug());
                if (attribute == null)
                    continue;
                var attributeValue = item.getAttributeSimple(attributeDef.getTechnicalName());
                extractAttributeValue(attributeValue, attribute);
            }
        }
        return item;
    }

    private void extractAttributeValue(AttributeSimpleValue attributeValue, Map<String, Object> attribute) {
        var elasticAttributeName = elasticLayout.buildElasticAttributeName(attributeValue.getAttributeDef());

        if (attribute.get(elasticAttributeName) != null) {
            elasticSupport.extractAttributeValue(attributeValue, attribute.get(elasticAttributeName));
        }

        // Load attributes metadata
        var attributeMetaMap = (Map<String, Object>) attribute.getOrDefault(StorageLayout.META_FIELD_NAME, new HashMap<>());

        for (Map.Entry<String, Object> metadata : attributeMetaMap.entrySet()) {
            var metadataDef = extractMetadataDef(metadata);
            if (metadataDef != null) { // We ignoring unknown metadata
                attributeValue.add(new MetadataValueDto(metadataDef, metadata.getValue()));
            }
        }
    }

    private MetadataDefDto extractMetadataDef(Map.Entry<String, Object> metadata) {
        if (!metadata.getKey().contains("_")) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Metadata name dest not contains _ :" + metadata);
        }
        String metaSlug = metadata.getKey().split("_", 2)[1];

        return metadataService.getBySlug(metaSlug);
    }
}