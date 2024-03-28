package com.provoly.ref.searchrequest;

import jakarta.persistence.*;

import com.provoly.common.search.Direction;
import com.provoly.ref.model.AttributeDef;

@Embeddable
public class Sort {
    @ManyToOne
    @JoinColumn(name = "sorted_attribute_id")
    private AttributeDef attribute;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    public AttributeDef getAttribute() {
        return attribute;
    }

    public void setAttribute(AttributeDef attribute) {
        this.attribute = attribute;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
