package com.provoly.transfo.engine;

public class TransfoStatusError {
    private final ErrorCode code;

    public TransfoStatusError(ErrorCode code) {
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    enum ErrorCode {
        MISSING_PROPERTY_IN_INPUT_ATTRIBUTE
    }

}
