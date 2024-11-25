package com.provoly.common.relation;

import com.provoly.common.model.RelationAttributes;

public enum RelationDirection {
    SOURCE(RelationAttributes.SOURCE),
    DESTINATION(RelationAttributes.DESTINATION);

    private final String attributeName;

    RelationDirection(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
