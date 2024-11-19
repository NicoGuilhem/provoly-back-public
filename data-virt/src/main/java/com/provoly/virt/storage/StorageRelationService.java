package com.provoly.virt.storage;

import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemsSearchResult;

public interface StorageRelationService {

    ItemsSearchResult getRelationsByItem(Item item);

    void loadRelations(ItemsSearchResult searchResult, int maxSize, boolean withSourceItems, boolean withDestinationItems);
}
