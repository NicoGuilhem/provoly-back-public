package com.provoly.virt.storage.elasticbased.elastic;

import static com.provoly.virt.storage.elasticbased.StorageLayout.DEFAULT_ORDER_NAME;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageSupport;
import com.provoly.virt.storage.elasticbased.AggregateQueryBuilder;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.core.SearchRequest;

@ApplicationScoped
class ElasticAggregateBuilder extends AggregateQueryBuilder {

    private Logger log;
    private ElasticSearchService elasticMonoClassSearch;

    public ElasticAggregateBuilder(Logger log, ElasticLayout storageLayout,
            @Any ElasticSearchService elasticMonoClassSearch, StorageSupport storageSupport) {
        super(storageSupport, storageLayout, log);
        this.elasticMonoClassSearch = elasticMonoClassSearch;
        this.log = log;
    }

    public SearchRequest.Builder buildAggregationRequest(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregation,
            MonoClassContextRequest monoClassContextRequest) {

        var orderName = DEFAULT_ORDER_NAME;
        var masterQuery = elasticMonoClassSearch.buildSearchSourceBuilder(classDto, request, monoClassContextRequest);

        var queryAggregation = buildAggregationQuery(aggregation, classDto, orderName, request.getLimit());
        log.infof("query aggregation : %s", queryAggregation);

        return new SearchRequest.Builder()
                .index(classDto.getSlug())
                .aggregations(queryAggregation)
                .query(masterQuery)
                .size(0);
    }
}
