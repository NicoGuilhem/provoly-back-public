package com.provoly.transfo.engine;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SimpleDto {

    private final String value;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SimpleDto(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
