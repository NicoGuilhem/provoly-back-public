package com.provoly.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.ElasticType;

/**
 * Type of variable.<br>
 * <b>/!\ This enum values are synchronized with IHM code/!\</b>
 */
public enum VariableType {
    INTEGER((value, allowedValues) -> Integer.parseInt(value)),
    STRING((value, allowedValues) -> {
    }),
    DOUBLE((value, allowedValues) -> Double.parseDouble(value)),
    DATE((value, allowedValues) -> LocalDate.parse(value)),
    UUID((value, allowedValues) -> java.util.UUID.fromString(value)),
    LIST((value, allowedValues) -> allowedValues.stream()
            .filter(allowedValue -> allowedValue.equals(value))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                    "Values %s isn't allowed.".formatted(value))));

    private final ValueAssignableToTypeCheck valueAssignableToTypeCheck;

    VariableType(ValueAssignableToTypeCheck valueAssignableToTypeCheck) {
        this.valueAssignableToTypeCheck = valueAssignableToTypeCheck;
    }

    public static ElasticType getElasticType(VariableType type) {
        return switch (type) {
            case INTEGER -> ElasticType.INTEGER;
            case STRING, UUID, LIST -> ElasticType.KEYWORD;
            case DOUBLE -> ElasticType.DOUBLE;
            case DATE -> ElasticType.DATE;
        };
    }

    @FunctionalInterface
    private interface ValueAssignableToTypeCheck {
        void check(String value, List<String> allowedValues);
    }

    public void checkValue(String value, List<String> allowedValues) {
        try {
            this.valueAssignableToTypeCheck.check(value, allowedValues);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new IllegalArgumentException("User profile value should be of type : %s".formatted(this), e);
        }
    }
}
