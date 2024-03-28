package com.provoly.common.transfo;

public class TransfoNodeErrorMissingAttribute extends TransfoNodeError {

    private final String attributeName;

    public TransfoNodeErrorMissingAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
