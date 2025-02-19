package com.provoly.virt.storage.elasticbased;

import static com.provoly.virt.storage.StorageSupport.*;
import static com.provoly.virt.storage.elasticbased.StorageLayout.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.DatasetVersionService;
import com.provoly.clients.MetadataRefService;
import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.CountDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.SearchAfterContext;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageSupport;

import io.kuzzle.sdk.coreClasses.SearchResult;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.internal.LazilyParsedNumber;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

@ApplicationScoped
public class KuzzleQueryResultService {

    public static final int AGGREGATION_PREFIX = 13; // remove the "Aggregation: " from the request
    public static final int QUERY_PREFIX = 7; // remove the "Query: " from the request
    private Logger log;
    private StorageSupport storageSupport;
    private MetadataRefService metadataService;

    private DatasetVersionService datasetVersionService;

    Predicate<AggregationParamDto> isMetric = aggregationParamDto -> aggregationParamDto.aggregatedBy() == null;

    public KuzzleQueryResultService(Logger log, StorageSupport storageSupport,
            @RestClient MetadataRefService metadataService,
            @RestClient DatasetVersionService datasetVersionService) {
        this.log = log;
        this.storageSupport = storageSupport;
        this.metadataService = metadataService;
        this.datasetVersionService = datasetVersionService;
    }

    public Map<String, Object> convertQueryToKuzzleQuery(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            Query query,
            KuzzleBasedLayout kuzzleBasedLayout) {

        Map<String, Object> finalQuery = new HashMap<>();
        if (query == null) {
            return finalQuery;
        }

        try {
            String queryString = query.toString().substring(QUERY_PREFIX);
            log.debugf("Built from elastic service %s", queryString);

            Map<String, Object> searchQuery = new ObjectMapper().readValue(queryString, Map.class);
            finalQuery.put("query", searchQuery);

            if (request.getSort() != null) {
                var sort = request.getSort();
                var propertyName = switch (sort.type()) {
                    case ATTRIBUTE -> {
                        var attribute = storageSupport.getAttributeById(classDto, sort.attribute());
                        if (attribute.getField().getType().isGeo()) {
                            throw new BusinessException(ErrorCode.BAD_REQUEST, "Sort is not allowed on geopoint field.");
                        }
                        yield kuzzleBasedLayout.buildAttributePath(attribute);
                    }
                    case METADATA -> kuzzleBasedLayout.buildElasticMetadataPath(metadataService.get(sort.attribute()));
                    case ITEM_ID -> kuzzleBasedLayout.getIdPath();
                };
                finalQuery.put("sort", List.of(Map.of(propertyName, sort.direction().name())));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to parse query", e);
        }

        log.debugf("final kuzzle query %s", finalQuery);
        return finalQuery;
    }

    public ItemsSearchResult convertToItemResult(SearchResult response,
            OClassDetailsDto oClass,
            KuzzleBasedLayout storageLayout) {
        var datasetVersionId = datasetVersionService.getAllActiveForClass(oClass.getId()).stream().toList().getFirst().getId();
        ItemsSearchResult result = new ItemsSearchResult();

        if (response == null) {
            // No more result or no result at all
            // We're returning an empty result
            return result;
        }

        for (var hit : response.hits) {
            Map<String, Object> map = (Map<String, Object>) hit.get("_source");
            map.put("_id", hit.get("_id"));

            var item = storageLayout.convertToItem(map, oClass, datasetVersionId);
            result.add(item);
        }

        result.setSearchAfter(new SearchAfterContext(response.getScrollId(), null));

        long totalSize = response.total;
        boolean isAccurate = totalSize <= 10000;
        log.debugv("Total size {0} - is size accurate : {1}", totalSize, isAccurate);
        result.setCount(Map.of(oClass.getId(), new CountDto(totalSize, isAccurate)));
        return result;
    }

    public Query buildSearchSourceBuilder(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest,
            SearchQueryBuilder queryBuilder,
            KuzzleBasedLayout layout) {
        log.debugf("Build search source builder");
        var masterQuery = QueryBuilders.bool();

        if (request.getCondition() != null) {
            var conditionQuery = queryBuilder.buildQuery(classDto, request.getCondition(),
                    monoClassContextRequest.securityMetaCondition());
            masterQuery.must(conditionQuery);
        }

        log.trace("Add security restriction");
        if (!monoClassContextRequest.securityCondition().composed.isEmpty()) { // If empty => user can see everything, no limitation
            var conditionQuery = queryBuilder.buildQuery(classDto, monoClassContextRequest.securityCondition());
            masterQuery.mustNot(conditionQuery);
        }

        if (classDto.getStorage() == Storage.KUZZLE) {
            log.trace("Add datasets restriction");
            masterQuery.must(queryBuilder.buildQuery(classDto, monoClassContextRequest.datasetsCondition()));
        }

        log.tracef("Add %s restriction", classDto.getStorage());
        var layoutCondition = layout.getLayoutConditions(classDto);

        if (layoutCondition != null) {
            masterQuery.must(queryBuilder.buildQuery(classDto, layoutCondition));
        }

        return Query.of(q -> q.bool(masterQuery.build()));
    }

    public Map<String, Object> buildKuzzleSearchQuery(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest,
            SearchQueryBuilder searchQueryBuilder,
            KuzzleBasedLayout layout) {
        Query query = buildSearchSourceBuilder(classDto, request, monoClassContextRequest, searchQueryBuilder, layout);
        return convertQueryToKuzzleQuery(classDto, request, query, layout);
    }

    public Map<String, Object> buildKuzzleSearchAggregateQuery(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregation,
            MonoClassContextRequest monoClassContextRequest,
            AggregateQueryBuilder aggregateQueryBuilder,
            SearchQueryBuilder searchQueryBuilder,
            KuzzleBasedLayout layout) {

        var finalQuery = buildKuzzleSearchQuery(classDto, request, monoClassContextRequest, searchQueryBuilder, layout);
        var queryAggregation = aggregateQueryBuilder
                .buildAggregationQuery(aggregation, classDto, DEFAULT_ORDER_NAME, request.getLimit());

        finalQuery.putAll(convertAggregationToKuzzleAggregation(queryAggregation));
        return finalQuery;
    }

    public Map<String, Object> convertAggregationToKuzzleAggregation(Map<String, Aggregation> queryAggregation) {
        if (queryAggregation.isEmpty()) {
            return Map.of();
        }

        String queryAggregationString = queryAggregation.get(AGGS)
                .toString()
                .substring(AGGREGATION_PREFIX);

        try {
            Map<String, Object> searchQuery = new ObjectMapper().readValue(queryAggregationString, Map.class);
            log.infof("query aggregation from elastic : %s", searchQuery);
            return Map.of(AGGS, Map.of(AGGS, searchQuery));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to parse query", e);
        }
    }

    public AggregationResultDto buildAggregationResultDto(AggregationParamDto aggregationParam, SearchResult searchResult,
            OClassDetailsDto classDto) {
        if (searchResult.getAggregations() == null) {
            log.debug("Aggregation empty, return the total of items");
            return new AggregationResultDto(aggregationParam.operation(),
                    List.of(new ItemAggregationDto.SimpleItemDto(RESULT_KEY, (long) searchResult.total)));
        }

        Map<String, Object> kuzzleAggregations = searchResult.getAggregations();

        if (isMetric.test(aggregationParam)) {
            log.debug("Aggregation result is a metric");
            return new AggregationResultDto(aggregationParam.operation(),
                    List.of(new ItemAggregationDto.SimpleItemDto(RESULT_KEY,
                            getValue(kuzzleAggregations, AGGS, aggregationParam.operation()))));
        }

        Map<String, Object> aggregations = (Map<String, Object>) kuzzleAggregations.get(AGGS);
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) aggregations.get("buckets");

        if (aggregationParam.groupBy() == null) {
            log.debugf("Aggregation result has a value field %s", aggregationParam.valueField());
            return new AggregationResultDto(aggregationParam.operation(), new ArrayList<>(buckets.stream()
                    .map(bucket -> new ItemAggregationDto.SimpleItemDto(
                            getKey(bucket, aggregationParam.aggregatedBy(), classDto),
                            getValue(bucket, OPERATION_AGGS, aggregationParam.operation())))
                    .toList()));
        }

        log.debugf("Aggregation result is grouped by %s", aggregationParam.groupBy());

        return new AggregationResultDto(aggregationParam.operation(), new ArrayList<>(buckets
                .stream()
                .map(bucket -> {
                    List<Map<String, Object>> groupedByBuckets = (List<Map<String, Object>>) ((Map<String, Object>) bucket
                            .get(GROUP_BY)).get("buckets");
                    List<ItemAggregationDto.SimpleItemDto> groupByValueDtos = groupedByBuckets
                            .stream()
                            .map(gbBucket -> new ItemAggregationDto.SimpleItemDto(
                                    getKey(gbBucket, aggregationParam.groupBy(), classDto),
                                    getValue(gbBucket, OPERATION_AGGS, aggregationParam.operation())))
                            .toList();
                    return new ItemAggregationDto.GroupedItemDto(getKey(bucket, aggregationParam.aggregatedBy(), classDto),
                            groupByValueDtos);
                })
                .toList()));
    }

    private Object getKey(Map<String, Object> bucket, UUID attribute, OClassDetailsDto oClassDetailsDto) {
        var attributeDetails = oClassDetailsDto
                .getAttributes()
                .stream()
                .filter(a -> a.getId().equals(attribute))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TECHNICAL,
                        "Attribute %s not found in oclass %s".formatted(attribute, oClassDetailsDto.getId())));

        return switch (attributeDetails.getField().getType()) {
            case INSTANT -> bucket.get("key_as_string");
            default -> bucket.get("key");
        };
    }

    private Object getValue(Map<String, Object> bucket, String key, AggregateOperation operation) {
        Map<String, Object> functionResult = (Map<String, Object>) bucket.get(key);

        return switch (operation) {
            case COUNT -> Long.parseLong(bucket.get("doc_count").toString());
            case MAX, MIN, AVG, SUM ->
                functionResult.isEmpty() ? 0.0 : Double.parseDouble(functionResult.get("value").toString());
            case Q1 -> getPercentileValue(functionResult, "25.0");
            case MEDIAN -> getPercentileValue(functionResult, "50.0");
            case Q3 -> getPercentileValue(functionResult, "75.0");
            case CARDINALITY -> functionResult.get("value").toString();
            case EXTENT -> functionResult.get("bounds") == null ? Map.of()
                    : parseBounds((Map<String, Map<String, LazilyParsedNumber>>) functionResult.get("bounds"));
        };
    }

    private Map<String, Map<String, Double>> parseBounds(Map<String, Map<String, LazilyParsedNumber>> bounds) {
        var result = new HashMap<String, Map<String, Double>>();
        bounds.forEach((key, value) -> result.put(key, mapToDouble(value)));
        return isEmptyBounds(result) ? Map.of() : result;
    }

    private boolean isEmptyBounds(Map<String, Map<String, Double>> parsedBound) {
        return parsedBound.get(TOP_LEFT).get(LON) == 0 &&
                parsedBound.get(TOP_LEFT).get(LAT) == 0 &&
                parsedBound.get(BOTTOM_RIGHT).get(LON) == 0 &&
                parsedBound.get(BOTTOM_RIGHT).get(LAT) == 0;
    }

    private Map<String, Double> mapToDouble(Map<String, LazilyParsedNumber> value) {
        return value.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().doubleValue()));
    }

    private double getPercentileValue(Map<String, Object> functionResult, String percentile) {
        return functionResult.isEmpty() ? 0.0
                : Double.parseDouble(((Map<String, Object>) functionResult.get("values")).get(percentile).toString());
    }
}
