package com.provoly.common.error;

import jakarta.ws.rs.core.Response;

public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode code, String message, Exception cause) {
        super(message, cause);
        this.code = code;
    }

    public BusinessException(ErrorDto error) {
        this(error.getCode(), error.getMessage());
    }

    public ErrorCode getCode() {
        return code;
    }

    public Response.Status getStatus() {
        return code.getStatus();
    }
}
