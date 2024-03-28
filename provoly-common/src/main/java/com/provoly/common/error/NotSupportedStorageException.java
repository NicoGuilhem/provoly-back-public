package com.provoly.common.error;

public class NotSupportedStorageException extends BusinessException {

    public NotSupportedStorageException(String message) {
        super(ErrorCode.NOT_SUPPORTED, message);
    }
}
