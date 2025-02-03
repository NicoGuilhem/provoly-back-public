package com.provoly.virt.storage.elasticbased.kuzzleasset;

import static com.provoly.virt.storage.elasticbased.kuzzleasset.KuzzleAssetLayout.ASSET_COLLECTION;

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

@StorageQualifier(Storage.KUZZLE_ASSET)
@ApplicationScoped
public class KuzzleAssetSearchService implements StorageSearchService {

    private Logger log;
    private KuzzleAssetSearchQueryBuilder searchQueryBuilder;
    private KuzzleAssetLayout layout;
    private KuzzleQueryResultService kuzzleQueryResultService;
    private KuzzleClient kuzzleClient;

    private ElasticSupport elasticSupport;
    private String tenant;

    public KuzzleAssetSearchService(Logger log,
            KuzzleAssetSearchQueryBuilder searchQueryBuilder,
            KuzzleAssetLayout layout,
            KuzzleQueryResultService kuzzleQueryResultService,
            KuzzleClient kuzzleClient, ElasticSupport elasticSupport) {
        this.log = log;
        this.searchQueryBuilder = searchQueryBuilder;
        this.layout = layout;
        this.kuzzleQueryResultService = kuzzleQueryResultService;
        this.kuzzleClient = kuzzleClient;
        this.elasticSupport = elasticSupport;
        this.tenant = kuzzleClient.getTenantName();
    }

    @Override
    public ItemsSearchResult search(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest) {
        elasticSupport.validateSearchLimit(request);
        var result = new ItemsSearchResult();
        var finalQuery = kuzzleQueryResultService.buildKuzzleSearchQuery(classDto, request, monoClassContextRequest,
                searchQueryBuilder, layout);

        if (finalQuery.isEmpty()) {
            // No request should be made return an empty resultset
            log.debugf("Query is null, return empty result");
            return result;
        }
        var response = kuzzleClient.kuzzleSearchPagination(tenant, ASSET_COLLECTION, finalQuery, request.getLimit(),
                request.getSearchAfter());
        return kuzzleQueryResultService.convertToItemResult(response, classDto, request, layout);

    }

}