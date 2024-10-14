package com.provoly.common.search;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DateInterval {
    SECOND,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    ;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }

    public static DateInterval from(String name) {
        return Optional.ofNullable(name).map(optionalName -> DateInterval.valueOf(optionalName.toUpperCase()))
                .orElse(null);
    }
}
