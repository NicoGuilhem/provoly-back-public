package com.provoly.virt.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.provoly.common.error.BusinessException;
import com.provoly.common.search.Operator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FilterDtoTest {

    @ParameterizedTest
    @MethodSource("allOperator")
    public void it_should_reject_filter_without_any_values(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("At least one value is required");
    }

    @ParameterizedTest
    @MethodSource("withUpperValueProvider")
    public void it_should_reject_INSIDE_OUTSIDE_operator_without_upper_value(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, "20"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Operator %s works with exactly two values".formatted(operator));
    }

    @ParameterizedTest
    @MethodSource("withUpperValueProvider")
    public void it_should_reject_INSIDE_OUTSIDE_operator_with_more_than_2_values(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, List.of("1", "2", "3")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Operator %s works with exactly two values".formatted(operator));

    }

    @ParameterizedTest
    @MethodSource("withUpperValueProvider")
    public void it_should_accept_INSIDE_OUTSIDE_operator_with_value(Operator operator) {
        assertThat(new FilterDto(UUID.randomUUID(), operator, "null", "null"))
                .isNotNull();
    }

    @ParameterizedTest
    @MethodSource("unsupportedMultiValuesOperatorProvider")
    public void it_should_reject_unsupported_operator_with_multi_values(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, List.of("1", "2", "3")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Operator %s allows only one value to be set".formatted(operator));
    }

    @ParameterizedTest
    @MethodSource("unsupportedMultiValuesOperatorProvider")
    public void it_should_reject_other_than_unsupported_operator_with_upper_value(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, "null", "20"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Operator %s allows only one value to be set".formatted(operator));
    }

    @ParameterizedTest
    @MethodSource("allOtherOperatorProvider")
    public void it_should_accept_other_operator_without_upper_value(Operator operator) {
        assertThat(new FilterDto(UUID.randomUUID(), operator, "20"))
                .isNotNull();
    }

    @ParameterizedTest
    @MethodSource("allOtherOperatorProvider")
    public void it_should_accept_other_operator_with_multi_values(Operator operator) {
        assertThat(new FilterDto(UUID.randomUUID(), operator, List.of("20", "20", "22", "24")))
                .isNotNull();
    }

    static Stream<Operator> withUpperValueProvider() {
        return Arrays.stream(Operator.values()).filter(Operator::isWithUpperValue);
    }

    static Stream<Operator> allOperator() {
        return Arrays.stream(Operator.values());
    }

    static Stream<Operator> unsupportedMultiValuesOperatorProvider() {
        return Arrays.stream(Operator.values()).filter(operator -> !operator.isMultiValued());
    }

    static Stream<Operator> allOtherOperatorProvider() {
        var unsupportedMultiValuesOperatorAndInsideOutside = withUpperValueProvider().collect(Collectors.toList());
        unsupportedMultiValuesOperatorAndInsideOutside.addAll(unsupportedMultiValuesOperatorProvider().toList());

        return Arrays.stream(Operator.values())
                .filter(operator -> !unsupportedMultiValuesOperatorAndInsideOutside.contains(operator));
    }

}