package com.provoly.common.error;

import jakarta.ws.rs.core.Response;

public enum ErrorCode {
    TECHNICAL(Response.Status.INTERNAL_SERVER_ERROR),
    NOT_FOUND(Response.Status.NOT_FOUND),
    CONFLICT(Response.Status.CONFLICT),
    ID_ALREADY_USED(Response.Status.CONFLICT),
    NAME_ALREADY_USED(Response.Status.CONFLICT),
    NOT_MODIFIABLE(Response.Status.FORBIDDEN),
    BAD_REQUEST(Response.Status.BAD_REQUEST),
    UNAUTHORIZED(Response.Status.UNAUTHORIZED),
    FORBIDDEN(Response.Status.FORBIDDEN),
    FIELD_KEY_NOT_FOUND,
    FILE_ALREADY_EXISTS,
    TOO_MANY_ERRORS,
    NOT_SUPPORTED(Response.Status.FORBIDDEN);

    private final Response.Status status;

    ErrorCode() {
        this.status = Response.Status.INTERNAL_SERVER_ERROR;
    }

    ErrorCode(Response.Status status) {
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }

}
