package com.provoly.virt.storage.elasticbased;

import static com.provoly.common.search.AggregateOperation.isOperationUsePercentile;
import static com.provoly.virt.storage.StorageSupport.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.virt.storage.StorageSupport;

import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.util.NamedValue;

@ApplicationScoped
public class AggregateQueryBuilder {

    private Logger log;
    private StorageSupport storageSupport;

    private StorageLayout storageLayout;

    public AggregateQueryBuilder(StorageSupport storageSupport, StorageLayout storageLayout, Logger log) {
        this.storageSupport = storageSupport;
        this.storageLayout = storageLayout;
        this.log = log;
    }

    public AggregateQueryBuilder() {
    }

    public Map<String, Aggregation> buildAggregationQuery(AggregationParamDto aggregation, OClassDetailsDto classDto,
            String orderName, int limit) {
        if (aggregation.aggregatedBy() == null && aggregation.valueField() == null) {
            log.debugf("Default aggregation will be the total number of items");
            return Map.of();
        }

        Aggregation valueFieldAggregation = getValueFieldAggregation(classDto, aggregation);

        if (valueFieldAggregation != null) {
            orderName = StorageLayout.OPERATION_AGGS;

            if (aggregation.aggregatedBy() == null) {
                log.debugf("Aggregation is a metric");
                return Map.of(StorageLayout.AGGS, valueFieldAggregation);
            }
        }

        var attributeDetail = storageSupport.getAttributeById(classDto, aggregation.aggregatedBy());

        log.infof("Build aggregation of aggregateBy %s that could be histogram, date histogram or simple graphic",
                attributeDetail.getName());
        var rootQuery = buildRootAggregation(aggregation, attributeDetail, orderName, limit);

        if (aggregation.groupBy() != null) {
            var attributeGroupBy = storageSupport.getAttributeById(classDto, aggregation.groupBy());
            log.infof("build aggregation query for groupBy attribute %s", attributeGroupBy.getName());

            String elasticFieldPaThGroupBy = storageLayout.buildAggregateAttributePath(attributeGroupBy);
            rootQuery.aggregations(StorageLayout.GROUP_BY,
                    Aggregation.of(aggs -> aggs.terms(term -> term.field(elasticFieldPaThGroupBy))
                            .aggregations(getValueFieldAggregationMap(valueFieldAggregation))));
        } else {
            rootQuery.aggregations(getValueFieldAggregationMap(valueFieldAggregation));
        }
        return Map.of(StorageLayout.AGGS, rootQuery.build());
    }

    private Map<String, Aggregation> getValueFieldAggregationMap(Aggregation valueFieldAggregation) {
        return valueFieldAggregation != null ? Map.of(StorageLayout.OPERATION_AGGS, valueFieldAggregation) : Map.of();
    }

    private Aggregation getValueFieldAggregation(OClassDetailsDto classDto, AggregationParamDto aggregation) {
        if (aggregation.valueField() == null) {
            return null;
        }
        var valueAttribute = storageSupport.getAttributeById(classDto, aggregation.valueField());
        log.infof("Build aggregation query for valueField %s with operation %s", valueAttribute.getName(),
                aggregation.operation());
        String ordinateElasticFieldPath = storageLayout.buildAggregateAttributePath(valueAttribute);

        return buildValueFieldAggregation(aggregation, valueAttribute, ordinateElasticFieldPath);
    }

    private Aggregation.Builder.ContainerBuilder buildRootAggregation(AggregationParamDto aggregation,
            AttributeDefDetailsDto aggregatedByAttribute, String aggregationName, int limit) {
        String elasticFieldPath = storageLayout.buildAggregateAttributePath(aggregatedByAttribute);
        if (aggregation.interval() != null) {
            storageSupport.checkFieldTypeIsNumeric(aggregatedByAttribute);
            log.debug("Aggregation is an histogram");
            return new Aggregation.Builder()
                    .histogram(h -> {
                        h.field(elasticFieldPath).interval(aggregation.interval());
                        addSort(h::order, aggregation, aggregationName);
                        return h;
                    });
        }

        if (aggregation.dateInterval() != null) {
            if (Type.from(aggregatedByAttribute.getField().type) != Type.INSTANT) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Aggregating on date is unavailable for attribute %s that is not a date."
                                .formatted(aggregatedByAttribute.getName()));
            }
            log.debug("Aggregation is a date histogram");
            var calendarIntervalValue = Stream.of(CalendarInterval.values())
                    .filter(value -> value.jsonValue().equals(aggregation.dateInterval().getName().toLowerCase()))
                    .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                            "%s is not a valid DateInterval value"
                                    .formatted(aggregation.dateInterval().getName())));

            return new Aggregation.Builder()
                    .dateHistogram(dh -> {
                        dh.field(elasticFieldPath)
                                .calendarInterval(calendarIntervalValue);
                        addSort(dh::order, aggregation, aggregationName);
                        return dh;
                    });
        }
        log.debug("Aggregation is a simple graphic (terms)");
        return new Aggregation.Builder().terms(t -> {
            t.field(elasticFieldPath)
                    .size(limit);
            addSort(t::order, aggregation, aggregationName);
            return t;
        });
    }

    private void addSort(Consumer<List<NamedValue<SortOrder>>> consumer,
            AggregationParamDto aggregation,
            String aggregationName) {
        if (aggregation.sortAggregates() != null && aggregation.sortAggregates().direction() != null
                && !isOperationUsePercentile(aggregation.operation())) {
            log.debugf("Adding order %s on aggregation %s", aggregation.sortAggregates().direction(), aggregation.operation());
            consumer.accept(List.of(getAggregationSort(aggregation.sortAggregates(), aggregationName)));
        }
    }

    private NamedValue<SortOrder> getAggregationSort(SortAggregate order, String aggregationName) {
        if (order.orderBy().equals(OrderBy.VALUE)) {
            return NamedValue.of(aggregationName, storageLayout.getSortDirection(order.direction()));
        }
        return NamedValue.of(StorageLayout.KEY_ORDER_NAME, storageLayout.getSortDirection(order.direction()));
    }

    private Aggregation buildValueFieldAggregation(AggregationParamDto aggregation,
            AttributeDefDetailsDto valueFieldAttr,
            String valueFieldElasticPath) {
        return switch (aggregation.operation()) {
            case COUNT -> Aggregation.of(agg -> agg.valueCount(m -> m.field(valueFieldElasticPath)));
            case MAX -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield Aggregation.of(agg -> agg.max(builder -> builder.field(valueFieldElasticPath)));
            }
            case MIN -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield Aggregation.of(agg -> agg.min(builder -> builder.field(valueFieldElasticPath)));
            }
            case AVG -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield Aggregation.of(agg -> agg.avg(builder -> builder.field(valueFieldElasticPath)));
            }
            case SUM -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield Aggregation.of(agg -> agg.sum(builder -> builder.field(valueFieldElasticPath)));
            }
            case CARDINALITY -> Aggregation.of(agg -> agg.cardinality(builder -> builder.field(valueFieldElasticPath)));
            case Q1 -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield getPercentile(valueFieldAttr, valueFieldElasticPath, 25.0);
            }
            case MEDIAN -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield getPercentile(valueFieldAttr, valueFieldElasticPath, 50.0);
            }
            case Q3 -> {
                storageSupport.checkFieldTypeIsNumeric(valueFieldAttr);
                yield getPercentile(valueFieldAttr, valueFieldElasticPath, 75.0);
            }
            case EXTENT -> {
                storageSupport.checkFieldTypeIsGeo(valueFieldAttr);
                yield Aggregation.of(agg -> agg.geoBounds(builder -> builder.field(valueFieldElasticPath)));
            }
        };
    }

    private Aggregation getPercentile(AttributeDefDetailsDto attribute, String elasticFieldPathY, double percentile) {
        storageSupport.checkFieldTypeIsNumeric(attribute);
        return Aggregation
                .of(agg -> agg.percentiles(builder -> builder.field(elasticFieldPathY).percents(List.of(percentile))));
    }

    public List<ItemAggregationDto> buildValuesByAttributeType(Aggregate result,
            AggregationParamDto params,
            Type attributeType,
            AttributeDefDetailsDto attributeGroupBy) {
        var operation = params.operation();
        log.infof("Build ItemAggregation list with operation %s", operation);

        if (params.interval() != null) {
            log.debug("Aggregation is an histogram");
            return getItemAggregationDtos(attributeGroupBy, operation, result.histogram(), HistogramBucket::key);
        }

        if (params.dateInterval() != null) {
            log.debug("Aggregation is a date histogram");
            return getItemAggregationDtos(attributeGroupBy, operation, result.dateHistogram(),
                    DateHistogramBucket::keyAsString);
        }

        log.debug("Aggregation is a simple graphic");
        return switch (attributeType) {
            case KEYWORD -> getItemAggregationDtos(attributeGroupBy, operation, result.sterms(),
                    stringTermsBucket -> stringTermsBucket.key()._get());
            case LONG, INTEGER ->
                getItemAggregationDtos(attributeGroupBy, operation, result.lterms(), LongTermsBucket::key);
            case DECIMAL ->
                getItemAggregationDtos(attributeGroupBy, operation, result.dterms(), DoubleTermsBucket::key);
            case INSTANT ->
                getItemAggregationDtos(attributeGroupBy, operation, result.lterms(), LongTermsBucket::keyAsString);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Aggregate by is not available for types %s, %s, or GEO type".formatted(Type.STRING, Type.RAW));
        };
    }

    private List<ItemAggregationDto.SimpleItemDto> getGroupByByAttributeType(MultiBucketBase term,
            String type,
            AggregateOperation operation) {
        return switch (Type.from(type)) {
            case KEYWORD -> getGroupByValueDtos(operation, getAggregation(term, StorageLayout.GROUP_BY).sterms(),
                    stringTermsBucket -> stringTermsBucket.key()._get());
            case LONG, INTEGER ->
                getGroupByValueDtos(operation, getAggregation(term, StorageLayout.GROUP_BY).lterms(), LongTermsBucket::key);
            case DECIMAL -> getGroupByValueDtos(operation, getAggregation(term, StorageLayout.GROUP_BY).dterms(),
                    DoubleTermsBucket::key);
            case INSTANT -> getGroupByValueDtos(operation, getAggregation(term, StorageLayout.GROUP_BY).lterms(),
                    LongTermsBucket::keyAsString);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "bad query");
        };
    }

    private <T extends MultiBucketBase> List<ItemAggregationDto> getItemAggregationDtos(AttributeDefDetailsDto attributeGroupBy,
            AggregateOperation operation,
            MultiBucketAggregateBase<T> aggregateBase,
            Function<T, Object> keyExtractor) {
        return aggregateBase
                .buckets()
                .array()
                .stream()
                .map(term -> buildItemAggregationWithGroupByOrValue(keyExtractor.apply(term), term, operation,
                        attributeGroupBy))
                .toList();
    }

    private <T extends MultiBucketBase> List<ItemAggregationDto.SimpleItemDto> getGroupByValueDtos(AggregateOperation operation,
            MultiBucketAggregateBase<T> aggregateBase,
            Function<T, Object> keyExtractor) {
        return aggregateBase
                .buckets()
                .array()
                .stream()
                .map(term -> {
                    if (!term.aggregations().containsKey(StorageLayout.OPERATION_AGGS)) {
                        return new ItemAggregationDto.SimpleItemDto(keyExtractor.apply(term), term.docCount());
                    }
                    return new ItemAggregationDto.SimpleItemDto(keyExtractor.apply(term),
                            getValueByOperation(getAggregation(term, StorageLayout.OPERATION_AGGS), operation));
                })
                .toList();
    }

    public Object getValueByOperation(Aggregate aggregate, AggregateOperation operation) {
        return switch (operation) {
            case COUNT -> aggregate.valueCount().value();
            case MAX -> aggregate.max().value();
            case MIN -> aggregate.min().value();
            case AVG -> aggregate.avg().value();
            case SUM -> aggregate.sum().value();
            case EXTENT -> {
                if (aggregate.geoBounds() == null || aggregate.geoBounds().bounds() == null) {
                    yield Map.of();
                }
                var bound = aggregate.geoBounds().bounds().tlbr();
                var topLeft = Map.of(
                        LAT, bound.topLeft().latlon().lat(),
                        LON, bound.topLeft().latlon().lon());
                var bottomRight = Map.of(
                        LAT, bound.bottomRight().latlon().lat(),
                        LON, bound.bottomRight().latlon().lon());
                yield Map.of(TOP_LEFT, topLeft, BOTTOM_RIGHT, bottomRight);

            }
            case CARDINALITY -> aggregate.cardinality().value();
            case MEDIAN, Q1, Q3 -> aggregate.tdigestPercentiles().values()
                    .keyed()
                    .values()
                    .stream()
                    .toList()
                    .getFirst();
        };

    }

    private static Aggregate getAggregation(MultiBucketBase term, String aggregationName) {
        return term.aggregations().get(aggregationName);
    }

    private ItemAggregationDto buildItemAggregationWithGroupByOrValue(Object key, MultiBucketBase term,
            AggregateOperation operation,
            AttributeDefDetailsDto attrGroupBy) {
        if (attrGroupBy != null) {
            return new ItemAggregationDto.GroupedItemDto(key,
                    getGroupByByAttributeType(term, attrGroupBy.getField().type, operation));
        }
        if (!term.aggregations().containsKey(StorageLayout.OPERATION_AGGS)) {
            return new ItemAggregationDto.SimpleItemDto(key, term.docCount());
        }
        return new ItemAggregationDto.SimpleItemDto(key,
                getValueByOperation(getAggregation(term, StorageLayout.OPERATION_AGGS), operation));
    }

}
