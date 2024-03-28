package com.provoly.common;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.ElasticType;

/**
 * Type of variable.<br>
 * <b>/!\ This enum values are synchronized with IHM code/!\</b>
 */
public enum VariableType {
    INTEGER,
    STRING,
    DOUBLE,
    DATE,
    UUID,
    LIST;

    public static ElasticType getElasticType(VariableType type) {
        switch (type) {
            case INTEGER:
                return ElasticType.INTEGER;
            case STRING:
            case UUID:
            case LIST:
                return ElasticType.KEYWORD;
            case DOUBLE:
                return ElasticType.DOUBLE;
            case DATE:
                return ElasticType.DATE;
        }
        throw new BusinessException(ErrorCode.TECHNICAL, "type not found");
    }

}
