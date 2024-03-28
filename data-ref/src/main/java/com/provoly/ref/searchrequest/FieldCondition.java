package com.provoly.ref.searchrequest;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.search.Operator;
import com.provoly.ref.model.Field;

@Entity
// TODO : Is a Field condition should be a Condition ?
public class FieldCondition {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    public MultiClassSearchRequest searchRequest;

    @ManyToOne
    private Field field;

    @Enumerated(EnumType.STRING)
    private Operator operator;
    private String value;

    public MultiClassSearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(MultiClassSearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
