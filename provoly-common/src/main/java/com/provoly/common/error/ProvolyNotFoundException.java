package com.provoly.common.error;

import java.util.UUID;

public class ProvolyNotFoundException extends BusinessException {

    public ProvolyNotFoundException(String msg) {
        super(ErrorCode.NOT_FOUND, msg);
    }

    public ProvolyNotFoundException(Class<?> type, UUID id) {
        this("%s : %s inexistant.".formatted(type.getSimpleName(), id));
    }

    public ProvolyNotFoundException(Class<?> type, String prop, String name) {
        this("%s with %s : %s inexistant.".formatted(type.getSimpleName(), prop, name));
    }
}
