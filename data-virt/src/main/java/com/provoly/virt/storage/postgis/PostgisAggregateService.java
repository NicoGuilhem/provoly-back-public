package com.provoly.virt.storage.postgis;

import static com.provoly.virt.storage.StorageSupport.BOTTOM_RIGHT;
import static com.provoly.virt.storage.StorageSupport.LAT;
import static com.provoly.virt.storage.StorageSupport.LON;
import static com.provoly.virt.storage.StorageSupport.TOP_LEFT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;
import com.provoly.virt.ProvolySpanManager;
import com.provoly.virt.search.mono.MonoClassContextRequest;
import com.provoly.virt.storage.StorageAggregateService;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageSupport;

import io.agroal.api.AgroalDataSource;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.postgis.jdbc.PGbox2d;

@ApplicationScoped
@StorageQualifier(Storage.POSTGIS)
public class PostgisAggregateService implements StorageAggregateService {
    private final Logger log;
    private final AgroalDataSource dataSource;
    private final PostgisSupport postgisSupport;

    private final StorageSupport storageSupport;

    private final ProvolySpanManager spanManager;

    private final ObjectMapper mapper;

    PostgisAggregateService(Logger log, AgroalDataSource dataSource, PostgisSupport postgisSupport,
            StorageSupport storageSupport, ObjectMapper mapper, ProvolySpanManager spanManager) {
        this.log = log;
        this.dataSource = dataSource;
        this.postgisSupport = postgisSupport;
        this.storageSupport = storageSupport;
        this.mapper = mapper;
        this.spanManager = spanManager;
    }

    @Override
    public AggregationResultDto aggregate(OClassDetailsDto classDto, MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {
        log.infof("Start an aggregation on %s class", classDto.getName());
        var span = spanManager.generateSpan("Start aggregation", Map.of("request", request.toString()));

        PostgisSelectQueryBuilder sql = buildAggregateQuery(aggregationParam, classDto, request, monoClassContextRequest);
        try (var conn = dataSource.getConnection();
                var statement = conn.prepareStatement(sql.toQuery());
                var rs = sql.executeQuery(statement)) {

            return buildResult(aggregationParam, rs);

        } catch (SQLException e) {
            span.recordException(e);
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to search in class Id :" + classDto.getId(), e);
        } finally {
            span.end();
        }
    }

    private AggregationResultDto buildResult(AggregationParamDto aggregationParam, ResultSet resultSet)
            throws SQLException {
        List<ItemAggregationDto> itemsItemAggregationDtos = new ArrayList<>();
        var aggregationResult = new AggregationResultDto(aggregationParam.operation(), itemsItemAggregationDtos);

        if (aggregationParam.groupBy() != null) {
            Map<Object, List<ItemAggregationDto.SimpleItemDto>> aggregationMap = new HashMap<>();
            while (resultSet.next()) {
                aggregationMap.putIfAbsent(resultSet.getObject(PostgisSupport.AGGREGATION_KEY), new ArrayList<>());
                aggregationMap.get(resultSet.getObject(PostgisSupport.AGGREGATION_KEY))
                        .add(new ItemAggregationDto.SimpleItemDto(resultSet.getObject(PostgisSupport.GROUPBY_KEY),
                                normalizeValue(resultSet.getObject(PostgisSupport.VALUE_KEY), aggregationParam.operation())));
            }
            aggregationMap.forEach((k, v) -> itemsItemAggregationDtos.add(new ItemAggregationDto.GroupedItemDto(k, v)));
        } else {
            while (resultSet.next()) {
                itemsItemAggregationDtos
                        .add(new ItemAggregationDto.SimpleItemDto(resultSet.getObject(PostgisSupport.AGGREGATION_KEY),
                                normalizeValue(resultSet.getObject(PostgisSupport.VALUE_KEY), aggregationParam.operation())));
            }
        }

        return aggregationResult;
    }

    private PostgisSelectQueryBuilder buildAggregateQuery(AggregationParamDto aggregationParam,
            OClassDetailsDto oClassDetailsDto,
            MonoClassRequestDto request, MonoClassContextRequest monoClassContextRequest) {

        aggregationParam = validateAggregation(aggregationParam, oClassDetailsDto, request);

        var valueFieldId = aggregationParam.valueField();

        String aggregateFieldName = null;
        String groupByName = null;
        String valueName;

        if (valueFieldId == null) {
            valueName = "*";
        } else {
            var valueAttribute = storageSupport.getAttributeById(oClassDetailsDto, valueFieldId);
            valueName = postgisSupport.getColumnName(valueAttribute);
        }

        if (aggregationParam.aggregatedBy() != null) {
            AttributeDefDetailsDto attributeDefDetailsDto = storageSupport.getAttributeById(oClassDetailsDto,
                    aggregationParam.aggregatedBy());
            aggregateFieldName = postgisSupport.getColumnName(attributeDefDetailsDto);
        }

        if (aggregationParam.groupBy() != null) {
            AttributeDefDetailsDto attributeDefDetailsDto = storageSupport.getAttributeById(oClassDetailsDto,
                    aggregationParam.groupBy());
            groupByName = postgisSupport.getColumnName(attributeDefDetailsDto);
        }

        var selectQueryBuilder = new PostgisSelectQueryBuilder(postgisSupport, mapper, log, storageSupport, oClassDetailsDto,
                spanManager);

        return selectQueryBuilder
                .with(aggregateFieldName, aggregationParam.interval())
                .with(aggregateFieldName, aggregationParam.dateInterval())
                .select(valueName, aggregationParam.operation(), aggregateFieldName, groupByName)
                .from()
                .where(request.getCondition(), null)
                .where(monoClassContextRequest.datasetsCondition(), null)
                .groupBy(aggregateFieldName, groupByName)
                .interval(aggregationParam.interval(), aggregationParam.operation(), groupByName)
                .dateInterval(aggregationParam.dateInterval(), aggregationParam.operation())
                .orderBy(aggregationParam.sortAggregates(), aggregateFieldName)
                .limit(request.getLimit());
    }

    private AggregationParamDto validateAggregation(AggregationParamDto aggregationParam, OClassDetailsDto oClassDetailsDto,
            MonoClassRequestDto request) {
        if (request.getSort() != null) {
            aggregationParam = new AggregationParamDto(aggregationParam);
        }
        if (aggregationParam.interval() != null) {
            AttributeDefDetailsDto aggregateBy = storageSupport.getAttributeById(oClassDetailsDto,
                    aggregationParam.aggregatedBy());
            storageSupport.checkFieldTypeIsNumeric(aggregateBy);
        }
        if (aggregationParam.dateInterval() != null) {
            AttributeDefDetailsDto aggregateBy = storageSupport.getAttributeById(oClassDetailsDto,
                    aggregationParam.aggregatedBy());
            storageSupport.checkFieldTypeIsDate(aggregateBy);
        }

        if (aggregationParam.operation() == null) {
            aggregationParam = new AggregationParamDto(aggregationParam.aggregatedBy());
        }

        AttributeDefDetailsDto valueAttribute = null;
        if (aggregationParam.valueField() != null) {
            valueAttribute = storageSupport.getAttributeById(oClassDetailsDto, aggregationParam.valueField());
        }

        switch (aggregationParam.operation()) {
            case MAX, MIN, AVG, SUM, MEDIAN, Q1, Q3 -> storageSupport.checkFieldTypeIsNumeric(valueAttribute);
            case EXTENT -> storageSupport.checkFieldTypeIsGeo(valueAttribute);
        }

        return aggregationParam;
    }

    private Object normalizeValue(Object value, AggregateOperation operation) {
        if (operation == AggregateOperation.EXTENT) {
            if (value == null) {
                return Map.of();
            }
            PGbox2d boxing = (PGbox2d) value;
            var topLeft = Map.of(
                    LAT, boxing.getURT().y,
                    LON, boxing.getLLB().x);
            var bottomRight = Map.of(
                    LAT, boxing.getLLB().y,
                    LON, boxing.getURT().x);
            value = Map.of(TOP_LEFT, topLeft, BOTTOM_RIGHT, bottomRight);
        }
        return value;
    }

}
