package com.provoly.virt.search.mono;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.*;
import com.provoly.virt.entity.AttributeSimpleValue;

@ApplicationScoped
public class ConditionsEvaluator {

    // Cannot use visitor pattern because of package strategy. ConditionDto in common and AttributeSimpleValue in virt
    public boolean conditionEvaluator(AttributeSimpleValue attribute, ConditionDto condition) {
        return switch (condition) {
            case TrueConditionDto unused -> true;
            case OrConditionDto orConditionDto -> conditionEvaluator(attribute, orConditionDto);
            case AndConditionDto andConditionDto -> conditionEvaluator(attribute, andConditionDto);
            case AttributeConditionDto attributeConditionDto -> conditionEvaluator(attribute, attributeConditionDto);
            case MetadataConditionDto metadataConditionDto -> conditionEvaluator(attribute, metadataConditionDto);
            default -> throw new IllegalStateException("Unexpected value: %s".formatted(condition));
        };
    }

    public boolean conditionEvaluator(AttributeSimpleValue attribute, OrConditionDto condition) {
        return condition.composed.stream().anyMatch(composedCondition -> conditionEvaluator(attribute, composedCondition));
    }

    public boolean conditionEvaluator(AttributeSimpleValue attribute, AndConditionDto condition) {
        return condition.composed.stream().anyMatch(composedCondition -> conditionEvaluator(attribute, composedCondition));
    }

    public boolean conditionEvaluator(AttributeSimpleValue attribute, MetadataConditionDto condition) {
        var expectedValue = condition.getValue();
        var actualValue = attribute.getMetadata(condition.getMetadata());

        return switch (condition.getOperator()) {
            case EQUALS -> actualValue != null && actualValue.isEqual(expectedValue);
            case NOT_EQUALS -> actualValue != null && !actualValue.isEqual(expectedValue);
            case EXISTS -> actualValue != null;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Metadata condition only supporting equals or not equals :" + condition.getOperator());
        };
    }

    public boolean conditionEvaluator(AttributeSimpleValue attribute, AttributeConditionDto condition) {
        throw new BusinessException(ErrorCode.TECHNICAL, "We should not have any attribute condition : " + attribute.getName());
    }
}
