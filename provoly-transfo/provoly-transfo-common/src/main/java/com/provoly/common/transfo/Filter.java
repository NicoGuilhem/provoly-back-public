package com.provoly.common.transfo;

import java.util.UUID;

import com.provoly.common.model.Type;
import com.provoly.common.search.Operator;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Filter extends NodeSpec {

    private final String attributeName;
    private final Operator operator;
    private final Object value;

    @JsonCreator
    public Filter(String attributeName, Operator operator, Object value) {
        super();
        this.attributeName = attributeName;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public TransfoNodeStatus validate(UUID nodeId, IntermediateModel inModel) {
        var outModel = inModel;
        var status = new TransfoNodeStatus(nodeId, outModel);

        validateAttribute(inModel, status);
        validateOperator(status);
        validateValue(status);

        return status;
    }

    private void validateAttribute(IntermediateModel inModel, TransfoNodeStatus status) {
        if (inModel == null) {
            return;
        }
        if (attributeName == null) {
            status.addError(new TransfoNodeErrorMissingProperty("attributeName"));
            return;
        }

        if (!inModel.hasAttribute(attributeName)) {
            status.addError(new TransfoNodeErrorMissingAttribute(attributeName));
            return;
        }

        var attributeType = inModel.getAttributeType(attributeName);
        if (attributeType != Type.INTEGER && attributeType != Type.LONG && attributeType != Type.DECIMAL) {
            status.addError(new TransfoNodeErrorBadType(attributeName));
        }
    }

    private void validateOperator(TransfoNodeStatus status) {
        if (operator == null) {
            status.addError(new TransfoNodeErrorMissingProperty("operator"));
        }
    }

    private void validateValue(TransfoNodeStatus status) {
        if (value == null) {
            status.addError(new TransfoNodeErrorMissingProperty("value"));
        }
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Operator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }
}
