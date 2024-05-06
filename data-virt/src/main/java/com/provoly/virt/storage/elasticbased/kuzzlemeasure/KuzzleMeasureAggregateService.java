package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

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

@StorageQualifier(Storage.KUZZLE_MEASURE)
@ApplicationScoped
public class KuzzleMeasureAggregateService implements StorageAggregateService {

    private Logger log;
    private KuzzleMeasureAggregateBuilder aggregateBuilder;
    private KuzzleClient kuzzleClient;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private KuzzleMeasureSearchQueryBuilder searchQueryBuilder;
    private KuzzleMeasureLayout layout;
    private String tenant;

    public KuzzleMeasureAggregateService(Logger log,
            KuzzleMeasureAggregateBuilder aggregateBuilder,
            KuzzleClient kuzzleClient,
            KuzzleQueryResultService kuzzleQueryResultService,
            KuzzleMeasureSearchQueryBuilder kuzzleMeasureSearchQueryBuilder, KuzzleMeasureLayout layout) {
        this.log = log;
        this.aggregateBuilder = aggregateBuilder;
        this.kuzzleClient = kuzzleClient;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.searchQueryBuilder = kuzzleMeasureSearchQueryBuilder;
        this.layout = layout;
        this.tenant = kuzzleClient.getTenantName();
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        var query = kuzzleQueryResultService.buildKuzzleSearchAggregateQuery(classDto, request, aggregationParam,
                monoClassContextRequest, aggregateBuilder, searchQueryBuilder, layout);
        var resp = kuzzleClient.kuzzleSearch(tenant, MEASURE_COLLECTION, query, 0);
        return kuzzleQueryResultService.buildAggregationResultDto(aggregationParam, resp, classDto);

    }

}
