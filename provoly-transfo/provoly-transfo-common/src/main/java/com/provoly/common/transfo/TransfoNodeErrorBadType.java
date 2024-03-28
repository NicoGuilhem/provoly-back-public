package com.provoly.common.transfo;

public class TransfoNodeErrorBadType extends TransfoNodeError {

    private final String attributeName;

    public TransfoNodeErrorBadType(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
