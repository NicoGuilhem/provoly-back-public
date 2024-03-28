package com.provoly.common.search;

import com.fasterxml.jackson.annotation.JsonCreator;

public class FullSearchConditionDto {

    private String value;

    @JsonCreator
    public FullSearchConditionDto(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
