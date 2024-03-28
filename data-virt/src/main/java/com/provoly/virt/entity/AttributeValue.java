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
        return attributeDef.field.getType();
    }

    public String getName() {
        return attributeDef.name;
    }

    public String getTechnicalName() {
        return attributeDef.technicalName;
    }

    public String getSlug() {
        return attributeDef.slug;
    }

    public AttributeDefDetailsDto getAttributeDef() {
        return attributeDef;
    }

}
