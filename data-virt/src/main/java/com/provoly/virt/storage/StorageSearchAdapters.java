package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassContextRequest;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageSearchAdapters implements StorageSearchService {

    private final Logger log;
    private final Instance<StorageSearchService> searchStorages;

    public StorageSearchAdapters(Logger log, @Any Instance<StorageSearchService> storages) {
        this.log = log;
        this.searchStorages = storages;
    }

    @Override
    public ItemsSearchResult search(OClassDetailsDto oClass, MonoClassRequestDto request, MonoClassContextRequest context) {
        log.debugf("Start a search in class %s", oClass.getId());
        return getService(searchStorages, oClass.getStorage()).search(oClass, request, context);
    }

}
