package com.provoly.virt.storage;

import java.util.Collection;

import com.provoly.common.relation.RelationDto;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.Relation;

public interface StorageRelationService {

    ItemsSearchResult getRelationsByItem(Item item);

    void loadRelations(ItemsSearchResult searchResult, boolean withSourceItems, boolean withDestinationItems);

    Collection<Relation> getRelationsByItemAndRelation(RelationDto relationDto);
}
