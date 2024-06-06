package com.provoly.virt.storage.elasticbased.elastic;

import static com.provoly.virt.storage.elasticbased.StorageLayout.AGGS;
import static com.provoly.virt.storage.elasticbased.StorageLayout.RESULT_KEY;

import java.io.IOException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageAggregateService;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSupport;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;

@StorageQualifier(Storage.ELASTIC)
@ApplicationScoped
class ElasticAggregateService implements StorageAggregateService {

    private Logger log;

    private ElasticsearchClient elastic;

    private ElasticAggregateBuilder elasticAggregateBuilder;

    private StorageSupport storageSupport;

    public ElasticAggregateService(Logger log,
            ElasticsearchClient elastic,
            ElasticAggregateBuilder elasticAggregateBuilder,
            StorageSupport storageSupport) {
        this.log = log;
        this.elastic = elastic;
        this.elasticAggregateBuilder = elasticAggregateBuilder;
        this.storageSupport = storageSupport;
    }

    public AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.info("Build search request that combine query and aggregations");

        try {
            var attributeDetail = storageSupport.getAttributeDetail(classDto, aggregationParam.aggregatedBy());
            var attributeGroupByDetail = storageSupport.getAttributeDetail(classDto, aggregationParam.groupBy());

            var response = elastic.search(
                    s -> elasticAggregateBuilder.buildAggregationRequest(classDto,
                            request,
                            aggregationParam,
                            monoClassContextRequest),
                    Void.class); // We do not care about matches (size is set to zero), using Void will ignore any document in the response.
            log.debug("Aggregation Request done");

            var result = response
                    .aggregations()
                    .get(AGGS);
            log.debugf("aggregation result : %s", result);

            if (aggregationParam.aggregatedBy() == null) {
                log.debugf("Aggregation is a metric");
                var value = getMetric(aggregationParam, response, result);
                return new AggregationResultDto(aggregationParam.operation(),
                        List.of(new ItemAggregationDto.SimpleItemDto(RESULT_KEY, value)));
            }

            List<ItemAggregationDto> items = elasticAggregateBuilder.buildValuesByAttributeType(result,
                    aggregationParam,
                    Type.from(attributeDetail.getField().type),
                    attributeGroupByDetail);

            return new AggregationResultDto(aggregationParam.operation(), items);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "An error occured during search", e);
        } catch (ElasticsearchException e) {
            log.infof("error : %s", e.response().error().rootCause());
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "An error occured: %s".formatted(e.response().error().rootCause()));
        }
    }

    private Object getMetric(AggregationParamDto aggregationParam, SearchResponse<Void> response, Aggregate result) {
        if (aggregationParam.valueField() == null) {
            return response.hits().total().value();
        }
        return elasticAggregateBuilder.getValueByOperation(result, aggregationParam.operation());
    }

}
