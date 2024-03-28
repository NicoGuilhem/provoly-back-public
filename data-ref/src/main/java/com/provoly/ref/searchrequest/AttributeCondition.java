package com.provoly.ref.searchrequest;

import jakarta.persistence.*;

import com.provoly.common.search.Operator;
import com.provoly.ref.model.AttributeDef;

@Entity
@DiscriminatorValue("ATTRIBUTE") // Should be same as ConditionType value
public class AttributeCondition extends Condition {

    @ManyToOne
    private AttributeDef attribute;

    @Enumerated(EnumType.STRING)
    private Operator operator;
    private String value;
    private String upperValue;
    private String location;

    public AttributeCondition() {
        // Only for JPA
    }

    public AttributeDef getAttribute() {
        return attribute;
    }

    public void setAttribute(AttributeDef attribute) {
        this.attribute = attribute;
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

    public String getUpperValue() {
        return upperValue;
    }

    public void setUpperValue(String upperValue) {
        this.upperValue = upperValue;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
