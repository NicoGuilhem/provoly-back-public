package com.provoly.virt.search.mono;

import java.util.List;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.search.OrConditionDto;

public record MonoClassContextRequest(OrConditionDto securityCondition, OrConditionDto securityMetaCondition,
        OrConditionDto datasetsCondition, List<AttributeDefDetailsDto> requestedAttributes) {

    @Override
    public String toString() {
        return "{" +
                "securityCondition: " + securityCondition +
                ", securityMetaCondition: " + securityMetaCondition +
                ", datasetsCondition:" + datasetsCondition +
                ", requestedAttributes:" + requestedAttributes +
                "} ";
    }

    public boolean isWithSecurityConditions() {
        return (securityCondition != null && securityCondition.composed != null && !securityCondition.composed.isEmpty())
                || (securityMetaCondition != null && securityMetaCondition.composed != null
                        && !securityMetaCondition.composed.isEmpty());
    }
}
