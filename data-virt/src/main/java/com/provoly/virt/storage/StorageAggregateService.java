package com.provoly.virt.storage;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.AggregationResultDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.search.mono.MonoClassContextRequest;

public interface StorageAggregateService {

    AggregationResultDto aggregate(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest);
}
