package com.provoly.common.search;

import java.util.Objects;

public record SortAggregate(Direction direction, OrderBy orderBy) {
    public SortAggregate {
        Objects.requireNonNull(direction);

        if (orderBy == null) {
            orderBy = OrderBy.VALUE;
        }
    }

    public static SortAggregate fromString(String param) {
        if (param == null) {
            return null;
        }
        String[] arg = param.split(",");

        Direction direction = Direction.valueOf(arg[0]);
        var orderBy = OrderBy.VALUE;
        if (arg.length == 2) {
            orderBy = OrderBy.valueOf(arg[1]);
        }

        return new SortAggregate(direction, orderBy);
    }
}
