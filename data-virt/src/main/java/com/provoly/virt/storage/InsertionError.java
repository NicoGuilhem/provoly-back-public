package com.provoly.virt.storage;

import com.provoly.common.error.BusinessException;

public record InsertionError(String itemId, String error, BusinessException cause) {

    public InsertionError(String itemId, String error) {
        this(itemId, error, null);
    }

    public InsertionError(String itemId, BusinessException cause) {
        this(itemId, cause.getMessage(), cause);
    }
}
