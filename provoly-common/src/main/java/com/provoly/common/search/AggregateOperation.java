package com.provoly.common.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AggregateOperation {
    COUNT,
    MAX,
    MIN,
    AVG,
    SUM,
    CARDINALITY,
    MEDIAN,
    Q1,
    Q3,
    EXTENT;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }

    public static AggregateOperation from(String name) {
        if (name == null) {
            return COUNT;
        }
        return AggregateOperation.valueOf(name.toUpperCase());
    }

    public static boolean isOperationUsePercentile(AggregateOperation operation) {
        return List.of(AggregateOperation.Q1, AggregateOperation.Q3, AggregateOperation.MEDIAN)
                .contains(operation);
    }
}
