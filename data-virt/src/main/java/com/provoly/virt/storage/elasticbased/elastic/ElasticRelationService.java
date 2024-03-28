package com.provoly.virt.storage.elasticbased.elastic;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.RelationAttributes;
import com.provoly.common.relation.RelationsAggregateDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.Relation;
import com.provoly.virt.item.ReadItemsService;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageRelationService;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@ApplicationScoped
@StorageQualifier(Storage.ELASTIC)
class ElasticRelationService implements StorageRelationService {

    public static final int MAX_SIZE = 10000;
    private DataVirtProperties properties;
    Logger log;

    ElasticsearchClient elasticClient;

    ReadItemsService itemService;

    public ElasticRelationService(Logger log,
            ElasticsearchClient elasticClient,
            ReadItemsService itemService,
            DataVirtProperties dataVirtProperties) {
        this.log = log;
        this.elasticClient = elasticClient;
        this.itemService = itemService;
        this.properties = dataVirtProperties;
    }

    /**
     * Methode permettant de charger les relations entre les items present dans ItemSearchResult.items
     *
     * @param searchResult
     */
    public void loadRelations(ItemsSearchResult searchResult) {
        try {
            log.infof("Starting search relations on %d objects", searchResult.size());
            if (searchResult.isEmpty()) {
                return; // No need to search for relation if no item in resultset
            }
            var itemsId = searchResult.getItems().stream()
                    .map(Item::getIdAsString)
                    .map(FieldValue::of)
                    .toList();
            // spotless:off
            var query = Query.of(q -> q
                    .bool(b -> b
                            .must(c1 -> c1
                                    .terms(t -> t
                                            .field("source")
                                            .terms(t2 -> t2.value(itemsId))))
                            .must(c1 -> c1
                                    .terms(t -> t
                                            .field("destination")
                                            .terms(t2 -> t2.value(itemsId))))));
            // spotless:on

            var response = executeRelationRequest(query, 100);
            for (var hit : response.hits().hits()) {
                String type = extractStringFrom(hit, RelationAttributes.TYPE);
                var sourceId = new ItemId(extractStringFrom(hit, RelationAttributes.SOURCE));
                var destinationId = new ItemId(extractStringFrom(hit, RelationAttributes.DESTINATION));
                searchResult.addRelation(type, sourceId, destinationId);
            }

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }

    }

    /**
     * Method to get all Relations and linked items using one Item. The Item passed to the method may be the source item of the
     * relation or the destination item
     *
     * @param item the item Object used to look for related relations
     * @return itemSearchResult, response object which have all items and relations found
     */
    public ItemsSearchResult getRelationsByItem(Item item) {
        try {
            // spotless:off
            var query = Query.of(q -> q
                    .bool(b -> b
                            .should(c1 -> c1
                                    .term(t -> t
                                            .field("source")
                                            .value(item.getIdAsString())))
                            .should(c1 -> c1
                                    .term(t -> t
                                            .field("destination")
                                            .value(item.getIdAsString())))));
            // spotless:on
            var response = executeRelationRequest(query, 100);
            return buildSearchResult(item, response);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }
    }

    Collection<Relation> getRelationsByAggregates(Collection<RelationsAggregateDto> aggregates) {
        try {
            var aggregatesIds = aggregates.stream()
                    .map(ra -> ra.aggregateId)
                    .map(FieldValue::of)
                    .toList();

            // spotless:off
            var query = Query.of(q -> q
                    .terms(t -> t
                            .field(RelationAttributes.AGGREGATE_ID)
                            .terms(ts -> ts.value(
                                    aggregatesIds))));
            // spotless:on

            var response = executeRelationRequest(query, MAX_SIZE);
            return buildRelationsFrom(response);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        }
    }

    private ItemsSearchResult buildSearchResult(Item targetItem, SearchResponse<Void> response) {
        ItemsSearchResult result = new ItemsSearchResult();
        result.add(targetItem);
        for (var hit : response.hits().hits()) {
            String type = extractStringFrom(hit, RelationAttributes.TYPE);
            var sourceId = new ItemId(extractStringFrom(hit, RelationAttributes.SOURCE));
            var destinationId = new ItemId(extractStringFrom(hit, RelationAttributes.DESTINATION));

            // FIXME : We should use a batch request to load every sourceId
            // FIXME : We should not use Itemservice
            var source = itemService.getOptional(sourceId);
            var destination = itemService.getOptional(destinationId);

            if (source.isPresent()) {
                result.add(source.get());
            } else {
                // Id should be undisclosed
                sourceId = ItemId.buildUndisclosed(sourceId);
            }

            if (destination.isPresent()) {
                result.add(destination.get());
            } else {
                // Id should be undisclosed
                destinationId = ItemId.buildUndisclosed(destinationId);
            }

            result.addRelation(type, sourceId, destinationId);
        }
        return result;
    }

    private Collection<Relation> buildRelationsFrom(SearchResponse<Void> response) {
        var result = new HashSet<Relation>();
        for (var hit : response.hits().hits()) {
            result.add(extractRelationFrom(hit));
        }
        return result;
    }

    private SearchResponse<Void> executeRelationRequest(Query query, int maxSize) throws IOException {
        if (log.isTraceEnabled()) {
            log.tracef("Executing relation request %s", query.toString());
        }

        return elasticClient.search(s -> s
                .index(properties.relationIndexName())
                .size(maxSize)
                .query(query)
                .fields(f -> f.field(RelationAttributes.TYPE))
                .fields(f -> f.field(RelationAttributes.SOURCE))
                .fields(f -> f.field(RelationAttributes.DESTINATION))
                .fields(f -> f.field(RelationAttributes.AGGREGATE_ID)), Void.class);
    }

    private Relation extractRelationFrom(Hit<Void> hit) {
        String type = extractStringFrom(hit, RelationAttributes.TYPE);
        var sourceId = new ItemId(extractStringFrom(hit, RelationAttributes.SOURCE));
        var destinationId = new ItemId(extractStringFrom(hit, RelationAttributes.DESTINATION));
        String aggregateId = extractStringFrom(hit, RelationAttributes.AGGREGATE_ID);
        return new Relation(type, sourceId, destinationId, aggregateId);
    }

    private static String extractStringFrom(Hit<Void> hit, String fieldName) {
        return hit.fields().get(fieldName).toJson().asJsonArray().getString(0);
    }

}
