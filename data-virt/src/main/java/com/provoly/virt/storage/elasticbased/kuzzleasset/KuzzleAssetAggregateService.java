package com.provoly.virt.storage.elasticbased.kuzzleasset;

import static com.provoly.virt.storage.elasticbased.StorageLayout.DEFAULT_ORDER_NAME;
import static com.provoly.virt.storage.elasticbased.kuzzleasset.KuzzleAssetLayout.ASSET_COLLECTION;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import com.provoly.common.Storage;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.AggregationResultDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.DataVirtProperties;
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
    private KuzzleAssetAggregateBuilder kuzzleAssetAggregateBuilder;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private KuzzleAssetSearchService kuzzleAssetSearchService;
    private String tenant;

    public KuzzleAssetAggregateService(Logger log,
            KuzzleAssetAggregateBuilder kuzzleAssetAggregateBuilder,
            KuzzleClient kuzzleClient,
            KuzzleQueryResultService kuzzleQueryResultService,
            @Any KuzzleAssetSearchService kuzzleAssetSearchService,
            DataVirtProperties dataVirtProperties) {
        this.log = log;
        this.kuzzleAssetAggregateBuilder = kuzzleAssetAggregateBuilder;
        this.kuzzleClient = kuzzleClient;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.kuzzleAssetSearchService = kuzzleAssetSearchService;
        this.tenant = dataVirtProperties.kuzzle().tenant().orElse("chalons");
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        var query = buildKuzzleRequest(classDto, request, aggregationParam, monoClassContextRequest);
        var resp = kuzzleClient.kuzzleSearch(tenant, ASSET_COLLECTION, query, 0);

        return kuzzleQueryResultService.buildAggregationResultDto(aggregationParam, resp, classDto);

    }

    public Map<String, Object> buildKuzzleRequest(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregation,
            MonoClassContextRequest monoClassContextRequest) {

        var finalQuery = kuzzleAssetSearchService.buildKuzzleSearchQuery(classDto, request, monoClassContextRequest);
        var queryAggregation = kuzzleAssetAggregateBuilder
                .buildAggregationQuery(aggregation, classDto, DEFAULT_ORDER_NAME, request.getLimit());
        finalQuery.putAll(kuzzleQueryResultService.convertAggregationToKuzzleAggregation(queryAggregation));
        return finalQuery;

    }
}
