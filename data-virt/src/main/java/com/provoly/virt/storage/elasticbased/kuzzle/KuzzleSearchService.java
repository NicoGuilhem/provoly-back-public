package com.provoly.virt.storage.elasticbased.kuzzle;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSearchService;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleClient;
import com.provoly.virt.storage.elasticbased.KuzzleQueryResultService;

import org.jboss.logging.Logger;

@StorageQualifier(Storage.KUZZLE)
@ApplicationScoped
public class KuzzleSearchService implements StorageSearchService {

    private Logger log;

    private KuzzleSearchQueryBuilder searchQueryBuilder;

    private KuzzleLayout kuzzleLayout;

    private KuzzleQueryResultService kuzzleQueryResultService;

    private KuzzleClient kuzzleClient;

    private ElasticSupport elasticSupport;

    public KuzzleSearchService(Logger log,
            KuzzleSearchQueryBuilder searchQueryBuilder,
            KuzzleLayout kuzzleLayout,
            KuzzleQueryResultService kuzzleQueryResultService,
            KuzzleClient kuzzleClient,
            ElasticSupport elasticSupport) {
        this.log = log;
        this.searchQueryBuilder = searchQueryBuilder;
        this.kuzzleLayout = kuzzleLayout;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.kuzzleClient = kuzzleClient;
        this.elasticSupport = elasticSupport;
    }

    @Override
    public ItemsSearchResult search(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {
        elasticSupport.validateSearchLimit(request);
        var result = new ItemsSearchResult();

        var query = kuzzleQueryResultService.buildSearchSourceBuilder(classDto, request, monoClassContextRequest,
                searchQueryBuilder, kuzzleLayout);

        if (query == null) {
            // No request should be made return an empty resultset
            log.debugf("Query is null, return empty result");
            return result;
        }

        Map<String, Object> finalQuery = kuzzleQueryResultService.convertQueryToKuzzleQuery(classDto, request, query,
                kuzzleLayout);

        var response = kuzzleClient.kuzzleSearch(classDto.getSlug(), KuzzleLayout.COLLECTION_NAME, finalQuery,
                request.getLimit());
        return kuzzleQueryResultService.convertToItemResult(response, classDto, request, kuzzleLayout);

    }
}