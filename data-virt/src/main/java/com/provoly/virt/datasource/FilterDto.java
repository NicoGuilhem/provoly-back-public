package com.provoly.virt.datasource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.Operator;

public record FilterDto(UUID attribute, Operator operator, List<String> values) {

    public FilterDto {
        if (values.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one value is required");
        }
        if (operator.isWithUpperValue() && values.size() != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Operator %s works with exactly two values".formatted(operator));
        }

        if (!operator.isMultiValued() && values.size() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Operator %s allows only one value to be set".formatted(operator));
        }
    }

    public FilterDto(UUID attribute, Operator operator, String value, String upperValue) {
        this(attribute, operator, List.of(value, upperValue));
    }

    public FilterDto(UUID attribute, Operator operator, String value) {
        this(attribute, operator, Collections.singletonList(value));
    }

    public String value() {
        return values.getFirst();
    }

    public String upperValue() {
        return values.get(1);
    }

    public static FilterDto fromString(String param) {
        if (param == null) {
            return null;
        }
        // splitting on "," with negative look behind on "\" to prevent splitting escaped commas ("\,")
        String[] arg = param.split("(?<!\\\\)" + Pattern.quote(","));

        var attribute = UUID.fromString(arg[0]);
        var operator = Operator.valueOf(arg[1]);

        String[] values = Arrays.copyOfRange(arg, 2, arg.length);
        // replacing escaped commas with commas (not escaped)
        List<String> listValues = Arrays.stream(values).map(value -> value.replaceAll("\\\\,", ",")).toList();
        return new FilterDto(attribute, operator, listValues);
    }
}
