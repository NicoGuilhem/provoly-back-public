package com.provoly.virt.storage.elasticbased.kuzzle;

import static com.provoly.virt.storage.elasticbased.kuzzle.KuzzleLayout.COLLECTION_NAME;

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

@StorageQualifier(Storage.KUZZLE)
@ApplicationScoped
public class KuzzleAggregateService implements StorageAggregateService {

    private Logger log;
    private KuzzleAggregateBuilder aggregateBuilder;
    private KuzzleSearchQueryBuilder searchQueryBuilder;
    private KuzzleLayout layout;
    private KuzzleClient kuzzleClient;
    private KuzzleQueryResultService kuzzleQueryResultService;

    public KuzzleAggregateService(Logger log,
            KuzzleAggregateBuilder aggregateBuilder,
            KuzzleSearchQueryBuilder searchQueryBuilder,
            KuzzleLayout layout,
            KuzzleClient kuzzleClient,
            KuzzleQueryResultService kuzzleQueryResultService) {
        this.log = log;
        this.aggregateBuilder = aggregateBuilder;
        this.searchQueryBuilder = searchQueryBuilder;
        this.layout = layout;
        this.kuzzleClient = kuzzleClient;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        var query = kuzzleQueryResultService.buildKuzzleSearchAggregateQuery(classDto, request, aggregationParam,
                monoClassContextRequest, aggregateBuilder, searchQueryBuilder, layout);
        var resp = kuzzleClient.kuzzleSearch(classDto.getSlug(), COLLECTION_NAME, query, 0);
        return kuzzleQueryResultService.buildAggregationResultDto(aggregationParam, resp, classDto);

    }

}
