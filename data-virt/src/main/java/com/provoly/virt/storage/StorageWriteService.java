package com.provoly.virt.storage;

import java.util.Collection;
import java.util.List;

import com.provoly.common.item.ItemUpdateMode;
import com.provoly.virt.entity.Item;

public interface StorageWriteService {
    List<InsertionError> addOrUpdate(Collection<Item> items, ItemUpdateMode updateMode);

}
