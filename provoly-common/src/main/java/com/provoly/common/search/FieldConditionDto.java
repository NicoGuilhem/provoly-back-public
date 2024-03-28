package com.provoly.common.search;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class FieldConditionDto {

    private final UUID field;
    private final String value;
    private String upperValue;
    private String location;
    private Operator operator;

    @JsonCreator
    @Default
    public FieldConditionDto(UUID field, String value) {
        this.field = field;
        this.value = value;
        this.operator = Operator.EQUALS;
    }

    public FieldConditionDto(UUID field, String value, String upperValue, String location, Operator operator) {
        this.field = field;
        this.value = value;
        this.upperValue = upperValue;
        this.location = location;
        this.operator = operator;
    }

    public UUID getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getUpperValue() {
        return upperValue;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "{" +
                "field: \"" + field + '"' +
                ", value: \"" + value + '"' +
                ", upperValue: \"" + upperValue + '"' +
                ", location: \"" + location + '"' +
                ", operator: \"" + operator + '"' +
                "} ";
    }
}
