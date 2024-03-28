package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.Storage;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemsSearchResult;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageRelationAdapters implements StorageRelationService {

    private final Logger log;
    private final Instance<StorageRelationService> storageRelationServices;

    public StorageRelationAdapters(Logger log, @Any Instance<StorageRelationService> storages) {
        this.log = log;
        this.storageRelationServices = storages;
    }

    @Override
    public ItemsSearchResult getRelationsByItem(Item item) {
        log.debugf("Deleting relation");
        return getService(storageRelationServices, Storage.ELASTIC).getRelationsByItem(item);
    }

    @Override
    public void loadRelations(ItemsSearchResult searchResult) {
        getService(storageRelationServices, Storage.ELASTIC).loadRelations(searchResult);
    }
}
