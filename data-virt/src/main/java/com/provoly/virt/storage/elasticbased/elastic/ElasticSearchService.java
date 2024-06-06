package com.provoly.virt.storage.elasticbased.elastic;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.common.search.SortDto;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.SearchAfterContext;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSearchService;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.ElasticSupport;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.PointInTimeReference;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;

@StorageQualifier(Storage.ELASTIC)
@ApplicationScoped
class ElasticSearchService implements StorageSearchService {
    public static final String KEEP_ALIVE_VALUE = "5m";
    public static final String EXPIRED_PIT_TTL_EXCEPTION = "search_context_missing_exception";
    private Logger log;

    private ElasticWriteService itemService;

    private ElasticsearchClient elastic;

    private ElasticSearchQueryBuilder queryBuilder;

    private ElasticFullTextQueryBuilder fullTextQueryBuilder;

    private ElasticLayout elasticLayout;

    private StorageSupport storageSupport;

    private ElasticSupport elasticSupport;
    private ElasticModelService elasticModelService;

    public ElasticSearchService(Logger log,
            @Any ElasticWriteService itemService,
            ElasticsearchClient elastic,
            ElasticSearchQueryBuilder queryBuilder,
            ElasticFullTextQueryBuilder fullTextQueryBuilder,
            ElasticLayout elasticLayout,
            StorageSupport storageSupport,
            ElasticSupport elasticSupport,
            @Any ElasticModelService elasticModelService) {
        this.log = log;
        this.itemService = itemService;
        this.elastic = elastic;
        this.queryBuilder = queryBuilder;
        this.fullTextQueryBuilder = fullTextQueryBuilder;
        this.elasticLayout = elasticLayout;
        this.storageSupport = storageSupport;
        this.elasticModelService = elasticModelService;
        this.elasticSupport = elasticSupport;
    }

    /**
     * monoClassContextRequest.securityMetadata must be applied on every attribute used in a condition,
     * in order to not filter on an attribute we are not allowed to see and let user guess the value
     *
     * @param classDto
     * @param request
     * @param monoClassContextRequest
     * @return
     */
    public ItemsSearchResult search(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {

        elasticSupport.validateSearchLimit(request);

        try {
            // Build request
            var query = buildSearchSourceBuilder(classDto, request, monoClassContextRequest);
            if (query == null) {
                // No request should be made return an empty resultset
                log.debugf("Query is null, return empty result");
                return new ItemsSearchResult();
            }

            // Executing request
            if (log.isTraceEnabled()) {
                log.tracef("Executing request %s", query);
            }
            var response = elastic.search(searchBuilder -> buildSearch(searchBuilder,
                    classDto,
                    request.getLimit(), // Elastic limit for number of items that can be returned
                    query,
                    request.getSort(),
                    request.getSearchAfter(),
                    monoClassContextRequest.requestedAttributes()),
                    Map.class);

            return itemService.convertToItemResult(response, classDto, request.isWithCount());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search", e);
        } catch (ElasticsearchException e) {
            log.infof("error : %s", e.response().error().rootCause());

            if (getSearchContextMissingException(e)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Unable to search due to time to live has expired. Please remove searchAfter to start over your search.");
            }

            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Unable to search due to %s".formatted(e.response().error().rootCause()));
        }
    }

    public Query buildSearchSourceBuilder(OClassDetailsDto classDto, MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {
        log.debug("Build search source builder");
        var masterQuery = QueryBuilders.bool();
        if (request.getFullSearch() != null) {
            var fullTextQuery = fullTextQueryBuilder.buildQuery(classDto, request.getFullSearch(),
                    monoClassContextRequest.securityMetaCondition());
            if (fullTextQuery == null) {
                return null; // We have a full search, but no field match => No result
            }
            masterQuery.must(fullTextQuery);
        }

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
        log.trace("Add datasets restriction");
        masterQuery.must(queryBuilder.buildQuery(classDto, monoClassContextRequest.datasetsCondition()));
        return Query.of(q -> q.bool(masterQuery.build()));
    }

    private SearchRequest.Builder buildSearch(SearchRequest.Builder searchRequestBuilder,
            OClassDetailsDto classDto,
            int limit,
            Query query,
            SortDto sort,
            String searchAfter,
            List<AttributeDefDetailsDto> attributesColumns) {

        var excludedColumns = recoveredExcludedColumns(classDto, attributesColumns);
        searchRequestBuilder
                .source(SourceConfig.of(s -> s.filter(f -> f.excludes(excludedColumns))))
                .query(query)
                .size(limit);

        boolean needIndex = true;

        if (sort != null) {
            addSortConfigToRequest(classDto, sort, searchRequestBuilder);

            if (!elasticLayout.containsItemId(sort)) { // if sort on item_id is provided, we don't use the point in time
                log.infof("There is no sort on item id, generate pit for index %s", classDto.getSlug());
                setPitInRequest(searchRequestBuilder, generatePointIntTime(classDto.getSlug()));
                needIndex = false;
            }

            if (searchAfter != null) {
                log.info("searchAfter property is present, convert it to SearchAfterContext");
                try {
                    SearchAfterContext searchAfterContext = storageSupport.getSearchAfterContext(searchAfter);
                    List<FieldValue> searchAfters = searchAfterContext
                            .searchAfter()
                            .stream()
                            .map(FieldValue::of)
                            .toList();

                    searchRequestBuilder.searchAfter(searchAfters);
                    if (searchAfterContext.pit() != null) {
                        log.info("Set the pit from search after context in the request");
                        setPitInRequest(searchRequestBuilder, searchAfterContext.pit());
                        needIndex = false;
                    }
                } catch (JsonProcessingException e) {
                    throw new BusinessException(ErrorCode.TECHNICAL, "Cannot deserialize search after into SearchAfterContext");
                }
            }
        }

        if (needIndex) {
            searchRequestBuilder.index(classDto.getSlug());
        }

        return searchRequestBuilder;
    }

    private List<String> recoveredExcludedColumns(OClassDetailsDto classDto, List<AttributeDefDetailsDto> attributesColumns) {
        var recoveredColumnsName = attributesColumns
                .stream()
                .map(AttributeDefDetailsDto::getTechnicalName)
                .toList();

        return classDto.getAttributes()
                .stream()
                .filter(att -> !attributesColumns.isEmpty() && !recoveredColumnsName.contains(att.getTechnicalName()))
                .map(att -> "%s".formatted(elasticLayout.buildAttributeRootPath(att)))
                .toList();
    }

    private void addSortConfigToRequest(OClassDetailsDto classDto, SortDto sort, SearchRequest.Builder searchRequestBuilder) {
        var elasticFieldPath = elasticLayout.getSortPath(classDto, sort);

        if (elasticModelService.isFieldMapped(classDto, elasticFieldPath)) {
            searchRequestBuilder
                    .sort(so -> so
                            .field(FieldSort.of(f -> f
                                    .field(elasticFieldPath)
                                    .order(elasticLayout.getSortDirection(sort.direction())))));
        }
    }

    private boolean getSearchContextMissingException(ElasticsearchException e) {
        return e.response().error().rootCause()
                .stream()
                .anyMatch(errorCause -> EXPIRED_PIT_TTL_EXCEPTION.equals(errorCause.type()));
    }

    private void setPitInRequest(SearchRequest.Builder searchRequestBuilder, String pit) {
        searchRequestBuilder.pit(PointInTimeReference.of(p -> p.id(pit).keepAlive(getKeepAliveValue())));
    }

    private String generatePointIntTime(String indexName) {
        try {
            return elastic.openPointInTime(pitBuilder -> pitBuilder.index(indexName).keepAlive(getKeepAliveValue())).id();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Error while generating pit for index : %s".formatted(indexName), e);
        }
    }

    private Time getKeepAliveValue() {
        return Time.of(t -> t.time(KEEP_ALIVE_VALUE));
    }

}
