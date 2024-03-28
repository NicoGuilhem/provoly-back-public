package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.AggregationParamDto;
import com.provoly.common.search.AggregationResultDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.search.mono.MonoClassContextRequest;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageAggregateAdapter implements StorageAggregateService {

    Instance<StorageAggregateService> aggregateStorages;
    Logger log;

    public StorageAggregateAdapter(Logger log, @Any Instance<StorageAggregateService> storages) {
        this.log = log;
        this.aggregateStorages = storages;
    }

    @Override
    public AggregationResultDto aggregate(OClassDetailsDto classDto, MonoClassRequestDto request,
            AggregationParamDto aggregationParam,
            MonoClassContextRequest monoClassContextRequest) {

        return getService(aggregateStorages, classDto.getStorage()).aggregate(classDto, request, aggregationParam,
                monoClassContextRequest);
    }

}
