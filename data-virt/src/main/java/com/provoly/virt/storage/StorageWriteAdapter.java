package com.provoly.virt.storage;

import static com.provoly.virt.storage.StorageAdapterUtils.getService;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import com.provoly.common.Storage;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.virt.entity.Item;

import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageWriteAdapter implements StorageWriteService {

    Instance<StorageWriteService> storageWriteAdapters;

    Logger log;

    public StorageWriteAdapter(Logger log, @Any Instance<StorageWriteService> storages) {
        this.log = log;
        this.storageWriteAdapters = storages;
    }

    @Override
    public List<InsertionError> addOrUpdate(Collection<Item> items, ItemUpdateMode updateMode) {
        List<Storage> storages = items.stream().map(i -> i.getoClass().getStorage()).toList(); // TODO: Should we change signature to get OClass ? Or alway check consistency even if was checked before ?
        return getService(storageWriteAdapters, storages.get(0)).addOrUpdate(items, updateMode);
    }

}
