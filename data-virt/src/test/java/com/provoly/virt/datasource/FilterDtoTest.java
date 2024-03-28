package com.provoly.virt.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import com.provoly.common.error.BusinessException;
import com.provoly.common.search.Operator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FilterDtoTest {

    @ParameterizedTest
    @MethodSource("insideOutsideProvider")
    public void it_should_reject_INSIDE_OUTSIDE_operator_with_upper_value(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, "20", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Missing upper value for operator %s".formatted(operator));
    }

    @ParameterizedTest
    @MethodSource("insideOutsideProvider")
    public void it_should_accept_INSIDE_OUTSIDE_operator_with_value(Operator operator) {
        assertThat(new FilterDto(UUID.randomUUID(), operator, null, "20"))
                .isNotNull();
    }

    @ParameterizedTest
    @MethodSource("allOtherOperatorProvider")
    public void it_should_reject_other_than_INSIDE_OUTSIDE_operator_with_upper_value(Operator operator) {
        assertThatThrownBy(() -> new FilterDto(UUID.randomUUID(), operator, null, "20"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Upper value is available only for operator INSIDE and OUTSIDE");
    }

    @ParameterizedTest
    @MethodSource("allOtherOperatorProvider")
    public void it_should_accept_other_than_INSIDE_OUTSIDE_operator_without_upper_value(Operator operator) {
        assertThat(new FilterDto(UUID.randomUUID(), operator, "20", null))
                .isNotNull();
    }

    static Stream<Operator> insideOutsideProvider() {
        return Stream.of(Operator.INSIDE, Operator.OUTSIDE);
    }

    static Stream<Operator> allOtherOperatorProvider() {
        return Arrays.stream(Operator.values()).filter(operator -> operator != Operator.INSIDE && operator != Operator.OUTSIDE);
    }

}