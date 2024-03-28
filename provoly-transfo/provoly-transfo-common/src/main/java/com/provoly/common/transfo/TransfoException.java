package com.provoly.common.transfo;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

/* This exception is thrown only if something seems to be a bug, not if transfo graph have a configuration error */
public class TransfoException extends BusinessException {
    public TransfoException(TransfoDto transfo, String message) {
        super(ErrorCode.TECHNICAL, message);
    }
}
