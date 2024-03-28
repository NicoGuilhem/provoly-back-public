package com.provoly.common.search;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.*;

public class AttributeConditionDto extends ConditionDto {

    private UUID attribute;
    private String value;
    private String upperValue;
    private String location;
    private Operator operator;

    @JsonCreator
    @Default
    public AttributeConditionDto(UUID attribute, String value, Operator operator) {
        this(attribute, value, null, null, operator);
    }

    public AttributeConditionDto(UUID attribute, Operator operator) {
        this(attribute, null, operator, null);
    }

    public AttributeConditionDto(UUID attribute, String value, String upperValue, String location, Operator operator) {
        this.type = ConditionType.ATTRIBUTE;
        this.attribute = attribute;
        this.value = value;
        this.upperValue = upperValue;
        this.location = location;
        this.operator = operator;
    }

    public AttributeConditionDto(UUID attribute, String value, Operator operator, String upperValue) {
        this(attribute, value, upperValue, null, operator);
    }

    public AttributeConditionDto(UUID attribute, String value, String location, Operator operator) {
        this(attribute, value, null, location, operator);
    }

    public UUID getAttribute() {
        return attribute;
    }

    public void setAttribute(UUID attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUpperValue() {
        return upperValue;
    }

    public void setUpperValue(String value) {
        this.upperValue = value;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Operator getOperator() {
        return operator == null ? Operator.EQUALS : operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "{" +
                "type: \"" + type + '"' +
                ", attribute: \"" + attribute + '"' +
                ", value: \"" + value + '"' +
                ", upperValue: \"" + upperValue + '"' +
                ", location: \"" + location + '"' +
                ", operator: \"" + operator + '"' +
                "} ";
    }

}
