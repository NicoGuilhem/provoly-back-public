package com.provoly.common.error;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonCreator;

@RegisterForReflection
public class ErrorDto {

    private final String application;
    private final ErrorCode code;
    private final String message;

    public ErrorDto(String application, BusinessException e) {
        this.application = application;
        this.code = e.getCode();
        this.message = e.getMessage();
    }

    @JsonCreator
    public ErrorDto(String application, ErrorCode code, String message) {
        this.application = application;
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return "ErrorDto{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }

    public String getApplication() {
        return application;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
