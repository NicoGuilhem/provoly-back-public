package com.provoly.virt.imports.model;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class ImportException extends BusinessException {
    public ImportException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ImportException(ErrorCode errorCode, String message, Exception e) {
        super(errorCode, message, e);
    }
}
