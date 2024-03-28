package com.provoly.common.datasource;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public record Search(@NotEmpty List<AttributeByDatasource> attributes, @NotNull String value, Integer limit) {

    //Just in case someone doesn't pass through validators
    public Search {
        if (attributes == null || attributes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Must contains at least one attribute.");
        }
        if (value == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Value search must not be null.");
        }
    }

    public static class AttributeByDatasource {
        UUID datasource;
        UUID attribute;

        public AttributeByDatasource(UUID attribute, UUID datasource) {
            this.attribute = attribute;
            this.datasource = datasource;
        }

        public UUID getAttribute() {
            return attribute;
        }

        public UUID getDatasource() {
            return datasource;
        }
    }
}
