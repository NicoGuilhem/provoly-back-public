package com.provoly.common.search;

import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.metadata.MetadataSystem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MetadataConditionDto extends ConditionDto {

    private UUID metadata;
    private String value;
    @JsonIgnore
    private String evaluatedValue; // Either the value evaluated if it's an EL or the value. Used to avoid references modifications and side effects #517
    private List<String> values;

    private Operator operator;

    @JsonCreator
    @Default
    public MetadataConditionDto(UUID metadata, String value, Operator operator) {
        this(metadata, value, null, operator);
    }

    public MetadataConditionDto(UUID metadata, String value, List<String> values, Operator operator) {
        this.type = ConditionType.METADATA;
        this.metadata = metadata;
        this.value = value;
        this.evaluatedValue = value; // some process does not evaluates condition as they do not permit Expression language value so it is necessary to init evaluatedValue with value. Change this behaviour with #517
        this.values = values;
        this.operator = operator == null ? Operator.EQUALS : operator;
    }

    public MetadataConditionDto(MetadataSystem metadata, String value, Operator operator) {
        this(metadata.getId(), value, null, operator);
    }

    public UUID getMetadata() {
        return metadata;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "{" +
                "type: \"" + type + '"' +
                ", metadata: \"" + metadata + '"' +
                ", value: \"" + value + '"' +
                ", values: \"" + values + '"' +
                ", operator: \"" + operator + '"' +
                "} ";
    }

    public String getEvaluatedValue() {
        return evaluatedValue;
    }

    public void setEvaluatedValue(String evaluatedValue) {
        this.evaluatedValue = evaluatedValue;
    }
}
