package com.provoly.common.search;

import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public record AggregationParamDto(
        UUID aggregatedBy,
        Double interval,
        DateInterval dateInterval,
        AggregateOperation operation,
        UUID valueField,
        UUID groupBy,
        SortAggregate sortAggregates) {

    public AggregationParamDto {
        if (aggregatedBy == null && (groupBy != null || interval != null || dateInterval != null)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AggregatedBy must be filled in");
        }
    }

    public AggregationParamDto(AggregationParamDto old, SortAggregate sortAggregate) {
        this(old.aggregatedBy(), old.interval(), old.dateInterval(), old.operation(), old.valueField(), old.groupBy(),
                sortAggregate);
    }

    public AggregationParamDto(AggregationParamDto old) {
        this(old.aggregatedBy(), old.interval(), old.dateInterval(), old.operation(), old.valueField(), old.groupBy(), null);
    }

    public AggregationParamDto() {
        this(null, null, null, AggregateOperation.COUNT, null, null, null);
    }

    public AggregationParamDto(UUID aggregatedBy) {
        this(aggregatedBy, null, null, AggregateOperation.COUNT, null, null, null);
    }

    public AggregationParamDto(UUID valueField, AggregateOperation operation) {
        this(null, null, null, operation, valueField, null, null);
    }

    public AggregationParamDto(UUID aggregatedBy, AggregateOperation operation, UUID valueField) {
        this(aggregatedBy, null, null, operation, valueField, null, null);
    }

    public AggregationParamDto(UUID aggregatedBy, AggregateOperation operation, UUID valueField, SortAggregate sortAggregates) {
        this(aggregatedBy, null, null, operation, valueField, null, sortAggregates);
    }

    public AggregationParamDto(UUID aggregatedBy, DateInterval dateInterval) {
        this(aggregatedBy, null, dateInterval, AggregateOperation.COUNT, null, null, null);
    }

    public AggregationParamDto(UUID aggregatedBy, double interval) {
        this(aggregatedBy, interval, null, AggregateOperation.COUNT, null, null, null);
    }

    public AggregationParamDto(UUID aggregatedBy, UUID groupBy) {
        this(aggregatedBy, null, null, AggregateOperation.COUNT, null, groupBy, null);
    }

    public AggregationParamDto(UUID aggregatedBy, AggregateOperation operation, UUID valueField, UUID groupBy) {
        this(aggregatedBy, null, null, operation, valueField, groupBy, null);
    }

    public AggregationParamDto(UUID aggregatedBy, AggregateOperation operation, UUID valueField, UUID groupBy,
            SortAggregate sortAggregates) {
        this(aggregatedBy, null, null, operation, valueField, groupBy, sortAggregates);
    }
}
