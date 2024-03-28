package com.provoly.virt.entity;

import java.util.ArrayList;
import java.util.Collection;

import com.provoly.common.model.AttributeDefDetailsDto;

public class AttributeMultiValue extends AttributeValue {

    private Collection<AttributeSimpleValue> values = new ArrayList<>();

    public AttributeMultiValue(Item parent, AttributeDefDetailsDto attributeDef) {
        super(parent, attributeDef);
    }

    public AttributeSimpleValue addValue() {
        return addValue(null);
    }

    public AttributeSimpleValue addValue(Object value) {
        AttributeSimpleValue simpleValue = new AttributeSimpleValue(getParent(), getAttributeDef(), value);
        values.add(simpleValue);
        return simpleValue;
    }

    public Iterable<AttributeSimpleValue> getValues() {
        return values;
    }

}
