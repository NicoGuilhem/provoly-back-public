package com.provoly.common.transfo;

public class TransfoNodeErrorMissingProperty extends TransfoNodeError {

    private final String propertyName;

    public TransfoNodeErrorMissingProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
