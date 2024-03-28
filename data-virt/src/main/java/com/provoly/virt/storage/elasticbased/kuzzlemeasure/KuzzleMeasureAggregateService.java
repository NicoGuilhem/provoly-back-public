package com.provoly.virt.storage.elasticbased.kuzzlemeasure;

import static com.provoly.virt.storage.elasticbased.StorageLayout.DEFAULT_ORDER_NAME;
import static com.provoly.virt.storage.elasticbased.kuzzlemeasure.KuzzleMeasureLayout.MEASURE_COLLECTION;

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

@StorageQualifier(Storage.KUZZLE_MEASURE)
@ApplicationScoped
public class KuzzleMeasureAggregateService implements StorageAggregateService {

    private Logger log;
    private KuzzleMeasureAggregateBuilder kuzzleMeasureAggregateBuilder;
    private KuzzleClient kuzzleClient;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private KuzzleMeasureSearchService kuzzleMeasureSearchService;
    private String tenant;

    public KuzzleMeasureAggregateService(Logger log,
            KuzzleMeasureAggregateBuilder kuzzleMeasureAggregateBuilder,
            KuzzleClient kuzzleClient,
            KuzzleQueryResultService kuzzleQueryResultService,
            @Any KuzzleMeasureSearchService kuzzleMeasureSearchService,
            DataVirtProperties dataVirtProperties) {
        this.log = log;
        this.kuzzleMeasureAggregateBuilder = kuzzleMeasureAggregateBuilder;
        this.kuzzleClient = kuzzleClient;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.kuzzleMeasureSearchService = kuzzleMeasureSearchService;
        this.tenant = dataVirtProperties.kuzzle().tenant().orElse("chalons");
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        var query = buildKuzzleRequest(classDto, request, aggregationParam, monoClassContextRequest);
        var resp = kuzzleClient.kuzzleSearch(tenant, MEASURE_COLLECTION, query, 0);

        return kuzzleQueryResultService.buildAggregationResultDto(aggregationParam, resp, classDto);

    }

    public Map<String, Object> buildKuzzleRequest(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregation,
            MonoClassContextRequest monoClassContextRequest) {

        var finalQuery = kuzzleMeasureSearchService.buildKuzzleSearchQuery(classDto, request, monoClassContextRequest);
        var queryAggregation = kuzzleMeasureAggregateBuilder
                .buildAggregationQuery(aggregation, classDto, DEFAULT_ORDER_NAME, request.getLimit());

        finalQuery.putAll(kuzzleQueryResultService.convertAggregationToKuzzleAggregation(queryAggregation));
        return finalQuery;
    }

}
