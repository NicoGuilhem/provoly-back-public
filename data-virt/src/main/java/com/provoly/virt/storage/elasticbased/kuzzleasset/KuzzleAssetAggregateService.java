package com.provoly.virt.storage.elasticbased.kuzzleasset;

import static com.provoly.virt.storage.elasticbased.kuzzleasset.KuzzleAssetLayout.ASSET_COLLECTION;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.AggregationResultDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageAggregateService;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.elasticbased.KuzzleClient;
import com.provoly.virt.storage.elasticbased.KuzzleQueryResultService;

import org.jboss.logging.Logger;

@StorageQualifier(Storage.KUZZLE_ASSET)
@ApplicationScoped
public class KuzzleAssetAggregateService implements StorageAggregateService {

    private Logger log;
    private KuzzleClient kuzzleClient;
    private KuzzleAssetAggregateBuilder aggregateBuilder;
    private KuzzleAssetSearchQueryBuilder searchQueryBuilder;
    private KuzzleAssetLayout layout;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private String tenant;

    public KuzzleAssetAggregateService(Logger log,
            KuzzleAssetAggregateBuilder aggregateBuilder,
            KuzzleClient kuzzleClient, KuzzleAssetSearchQueryBuilder searchQueryBuilder, KuzzleAssetLayout layout,
            KuzzleQueryResultService kuzzleQueryResultService) {
        this.log = log;
        this.aggregateBuilder = aggregateBuilder;
        this.kuzzleClient = kuzzleClient;
        this.searchQueryBuilder = searchQueryBuilder;
        this.layout = layout;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.tenant = kuzzleClient.getTenantName();
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        var query = kuzzleQueryResultService.buildKuzzleSearchAggregateQuery(classDto, request, aggregationParam,
                monoClassContextRequest, aggregateBuilder, searchQueryBuilder, layout);
        var resp = kuzzleClient.kuzzleSearch(tenant, ASSET_COLLECTION, query, 0);
        return kuzzleQueryResultService.buildAggregationResultDto(aggregationParam, resp, classDto);

    }

}
