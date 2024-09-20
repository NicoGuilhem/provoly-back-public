package com.provoly.virt.storage.elasticbased.kuzzle;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

public class KuzzleUnconfiguredClient extends BusinessException {
    public KuzzleUnconfiguredClient() {
        super(ErrorCode.TECHNICAL, "Can't use Kuzzle client as it is not configured.");
    }
}
