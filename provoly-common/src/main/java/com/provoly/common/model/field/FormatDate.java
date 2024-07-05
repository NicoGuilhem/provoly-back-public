package com.provoly.common.model.field;

import java.util.Arrays;

public enum FormatDate {
    DATETIME,
    DATE,
    MONTH_YEAR,
    DAY_MONTH,
    YEAR,
    MONTH,
    DAY;

    public static boolean isKnownFormat(String value) {
        return value != null &&
                Arrays.stream(values())
                        .anyMatch(date -> date.name().equals(value));

    }
}
