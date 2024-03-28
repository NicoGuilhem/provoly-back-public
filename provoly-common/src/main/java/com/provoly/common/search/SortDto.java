package com.provoly.common.search;

import java.util.Objects;
import java.util.UUID;

public record SortDto(boolean sortById, UUID attribute, Direction direction, SortType type) {
    public SortDto {
        Objects.requireNonNull(direction);

        if (sortById) {
            type = SortType.ITEM_ID;
        } else {
            Objects.requireNonNull(attribute);
        }

        if (type == null) {
            type = SortType.ATTRIBUTE;
        }
    }

    public SortDto(UUID attribute, Direction direction) {
        this(false, attribute, direction, SortType.ATTRIBUTE);
    }

    public SortDto(boolean isSortId, Direction direction) {
        this(isSortId, null, direction, null);
    }

    public SortDto(boolean isSortId) {
        this(isSortId, null, Direction.asc, null);
    }

    public SortDto(UUID attribute, Direction direction, SortType sortType) {
        this(false, attribute, direction, sortType);
    }

    public SortDto(UUID attribute, SortType type) {
        this(false, attribute, Direction.asc, type);
    }

    public SortDto(UUID attribute) {
        this(false, attribute, Direction.asc, SortType.ATTRIBUTE);
    }

    public static SortDto fromString(String param) {
        if (param == null) {
            return null;
        }
        var direction = Direction.asc;
        var type = SortType.ATTRIBUTE;
        UUID attribute = null;
        boolean sortById = false;

        String[] arg = param.split(",");
        try {
            attribute = UUID.fromString(arg[0]);
        } catch (IllegalArgumentException e) {
            sortById = Boolean.parseBoolean(arg[0]);
        }

        if (arg.length == 2) {
            direction = Direction.valueOf(arg[1]);
        }
        if (!sortById && arg.length == 3) {
            type = SortType.valueOf(arg[2]);
        }

        return new SortDto(sortById, attribute, direction, type);
    }

    @Override
    public String toString() {
        return "{" +
                "attribute: \"" + type + '"' +
                ", direction: \"" + direction + '"' +
                ", type: \"" + type + '"' +
                "} ";
    }
}
