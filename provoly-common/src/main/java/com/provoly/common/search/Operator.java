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
    IN(true),
    NOT_IN(true),

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

    public boolean isWithNativeListOfValues() {
        return switch (this) {
            case IN, NOT_IN -> true;
            default -> false;
        };
    }

    /**
     * Is this operator compatible with multiple values ?<br>
     * <br>
     * Examples :<br>
     * <ul>
     * <li>
     * if filter is : <code>var,EQUALS,12,35,44</code>, condition will be <code>var = 12 OR var = 35 OR var = 44</code>
     * </li>
     * <li>
     * if filter is : <code>var,IN,12,35,44</code>, condition will be <code>var IN (12, 35, 44)</code>
     * </li>
     * <li>
     * if filter is : <code>var,LOWER_THAN,12,35,44</code>, instantiating a FilterDto will raise an exception as operator does
     * not support multi values
     * </li>
     * </ul>
     *
     * @return <code>true</code> if compatible because operation supports list of values (like IN or NOT_IN)
     *         or if operator can be combine in multiple conditions joined by OR.
     */
    public boolean isMultiValued() {
        return multiValued;
    }
}
