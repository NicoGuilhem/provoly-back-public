package com.provoly.virt.storage.elasticbased.elastic;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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

    private DataVirtProperties properties;
    private Logger log;

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
     * Methode permettant de charger les relations entre les items presents dans {@code ItemSearchResult.items }
     *
     * @param searchResult les items dont on veut charger les relations
     */
    public void loadRelations(ItemsSearchResult searchResult) {
        try {
            log.infof("Starting search relations on %d objects", searchResult.size());
            if (searchResult.isEmpty()) {
                return; // No need to search for relation if no item in resultset
            }
            var query = buildRelationQuery(searchResult.getItems());
            var response = executeRelationRequest(query, 100);
            for (var hit : response.hits().hits()) {
                String type = extractStringFrom(hit, RelationAttributes.TYPE);
                var sourceId = new ItemId(extractStringFrom(hit, RelationAttributes.SOURCE));
                var destinationId = new ItemId(extractStringFrom(hit, RelationAttributes.DESTINATION));
                searchResult.addRelation(type, sourceId, destinationId);
            }

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search relations of searchResult", e);
        }

    }

    /**
     * Builds the query to search for relations of a list of items.<br>
     * A relation is retrieved if the item is the source or the destination of the relation.
     * 
     * @param items the list of items to search for relations
     * @return the query to search for relations
     */
    private Query buildRelationQuery(List<Item> items) {

        var itemsId = items.stream()
                .map(Item::getIdAsString)
                .map(FieldValue::of)
                .toList();

        return Query.of(q -> q
                .bool(b -> b
                        .should(c1 -> c1
                                .terms(t -> t
                                        .field(RelationAttributes.SOURCE)
                                        .terms(t2 -> t2.value(itemsId))))
                        .should(c1 -> c1
                                .terms(t -> t
                                        .field(RelationAttributes.DESTINATION)
                                        .terms(t2 -> t2.value(itemsId))))));
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
            var query = buildRelationQuery(List.of(item));
            var response = executeRelationRequest(query, 100);
            return buildSearchResult(item, response);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search relations of item", e);
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

            var response = executeRelationRequest(query, properties.maxSizeLimit());
            return buildRelationsFrom(response);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search relations by aggregates", e);
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
