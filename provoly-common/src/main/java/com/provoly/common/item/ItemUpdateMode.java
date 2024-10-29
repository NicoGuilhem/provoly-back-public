package com.provoly.common.item;

import java.util.Objects;

public enum ItemUpdateMode {
    REPLACE,
    MERGE;

    public static ItemUpdateMode fromString(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return ItemUpdateMode.valueOf(value.toUpperCase());
    }
}
