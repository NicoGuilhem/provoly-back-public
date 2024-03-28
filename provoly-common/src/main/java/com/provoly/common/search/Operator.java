package com.provoly.common.search;

public enum Operator {
    EQUALS,
    I_EQUALS,
    NOT_EQUALS,
    I_NOT_EQUALS,
    CONTAINS,
    I_CONTAINS,
    START_WITH,
    I_START_WITH,
    END_WITH,
    I_END_WITH,
    GREATER_THAN,
    LOWER_THAN,
    INSIDE,
    OUTSIDE,
    DISTANCE,

    // Partial implementation of Exists needed in security
    EXISTS,
    INTERSECTS
}
