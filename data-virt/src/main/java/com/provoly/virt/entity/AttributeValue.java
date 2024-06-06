package com.provoly.virt.entity;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.Type;

public abstract class AttributeValue {

    private final Item parent;
    private final AttributeDefDetailsDto attributeDef;

    public AttributeValue(Item parent, AttributeDefDetailsDto attributeDef) {
        this.parent = parent;
        this.attributeDef = attributeDef;
    }

    public Item getParent() {
        return parent;
    }

    public Type getFieldType() {
        return attributeDef.getField().getType();
    }

    public String getName() {
        return attributeDef.getName();
    }

    public String getTechnicalName() {
        return attributeDef.getTechnicalName();
    }

    public String getSlug() {
        return attributeDef.getSlug();
    }

    public AttributeDefDetailsDto getAttributeDef() {
        return attributeDef;
    }

}
