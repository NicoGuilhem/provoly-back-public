package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSearchService;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleClient;
import com.provoly.virt.storage.elasticbased.KuzzleQueryResultService;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

@StorageQualifier(Storage.KUZZLE_MEASURE)
@ApplicationScoped
public class KuzzleMeasureSearchService implements StorageSearchService {

    private Logger log;
    private KuzzleMeasureSearchQueryBuilder searchQueryBuilder;
    private KuzzleMeasureLayout kuzzleMeasureLayout;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private KuzzleClient kuzzleClient;
    private String tenant;
    private ElasticSupport elasticSupport;

    public KuzzleMeasureSearchService(Logger log,
            KuzzleMeasureSearchQueryBuilder searchQueryBuilder,
            KuzzleMeasureLayout kuzzleMeasureLayout,
            KuzzleQueryResultService kuzzleQueryResultService,
            KuzzleClient kuzzleClient,
            DataVirtProperties dataVirtProperties,
            ElasticSupport elasticSupport) {
        this.log = log;
        this.searchQueryBuilder = searchQueryBuilder;
        this.kuzzleMeasureLayout = kuzzleMeasureLayout;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.kuzzleClient = kuzzleClient;
        this.tenant = dataVirtProperties.kuzzle().tenant().orElse("chalons");
        this.elasticSupport = elasticSupport;
    }

    @Override
    public ItemsSearchResult search(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {
        elasticSupport.validateSearchLimit(request);
        var result = new ItemsSearchResult();
        var finalQuery = buildKuzzleSearchQuery(classDto, request, monoClassContextRequest);

        if (finalQuery.isEmpty()) {
            // No request should be made return an empty resultset
            log.debugf("Query is null, return empty result");
            return result;
        }
        var response = kuzzleClient.kuzzleSearch(tenant, MEASURE_COLLECTION, finalQuery, request.getLimit());
        return kuzzleQueryResultService.convertToItemResult(response, classDto, request, kuzzleMeasureLayout);

    }

    public Map<String, Object> buildKuzzleSearchQuery(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {
        Query query = kuzzleQueryResultService.buildSearchSourceBuilder(classDto, request,
                monoClassContextRequest, searchQueryBuilder, kuzzleMeasureLayout);
        return kuzzleQueryResultService.convertQueryToKuzzleQuery(classDto, request, query, kuzzleMeasureLayout);
    }

}