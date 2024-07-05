package com.provoly.common.model.field;

import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class FieldDateDto extends FieldDto {
    private String format;

    public FieldDateDto(UUID id, String name, String type, String slug, String format) {
        super(id, name, type, slug);
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public void checkField() throws BusinessException {
        if (!FormatDate.isKnownFormat(getFormat())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Format %s is not allowed.".formatted(getFormat()));
        }
    }
}
