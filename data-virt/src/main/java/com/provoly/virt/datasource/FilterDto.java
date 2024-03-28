package com.provoly.virt.datasource;

import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.Operator;

public record FilterDto(UUID attribute, Operator operator, String value, String upperValue) {

    public FilterDto {
        if (List.of(Operator.INSIDE, Operator.OUTSIDE).contains(operator) && upperValue == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Missing upper value for operator %s".formatted(operator));
        }

        if (!List.of(Operator.INSIDE, Operator.OUTSIDE).contains(operator) && upperValue != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Upper value is available only for operator %s and %s".formatted(Operator.INSIDE, Operator.OUTSIDE));
        }
    }

    public static FilterDto fromString(String param) {
        if (param == null) {
            return null;
        }
        String[] arg = param.split(",");

        var attribute = UUID.fromString(arg[0]);
        var operator = Operator.valueOf(arg[1]);
        String value = arg[2];
        String upperValue = null;

        if (arg.length == 4) {
            upperValue = arg[3];
        }

        return new FilterDto(attribute, operator, value, upperValue);
    }
}
