package com.provoly.virt.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.NotSupportedStorageException;

@ApplicationScoped
class StorageAdapterUtils {
    private StorageAdapterUtils() {
    }

    static <T> T getService(Instance<T> services, Storage storage) {
        try {
            T adapter = services.select(StorageQualifier.StorageLiteral.of(storage)).get();
            if (adapter == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        "This action is not implemented yet for %s storage".formatted(storage));
            }
            return adapter;
        } catch (UnsatisfiedResolutionException e) {
            throw new NotSupportedStorageException("This action is not implemented yet for %s storage".formatted(storage));
        }
    }
}
