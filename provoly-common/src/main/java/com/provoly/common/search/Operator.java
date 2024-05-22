package com.provoly.common.search;

public enum Operator {
    EQUALS(true),
    I_EQUALS(true),
    NOT_EQUALS(true),
    I_NOT_EQUALS(true),
    CONTAINS(true),
    I_CONTAINS(true),
    START_WITH(true),
    I_START_WITH(true),
    END_WITH(true),
    I_END_WITH(true),
    GREATER_THAN(false),
    LOWER_THAN(false),
    INSIDE(true),
    OUTSIDE(true),
    DISTANCE(false),

    // Partial implementation of Exists needed in security
    EXISTS(true),
    INTERSECTS(false);

    private final boolean multiValued;

    Operator(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public boolean isWithUpperValue() {
        return switch (this) {
            case INSIDE, OUTSIDE -> true;
            default -> false;
        };
    }

    public boolean isMultiValued() {
        return multiValued;
    }
}
