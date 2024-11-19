package com.provoly.virt.entity;

import java.util.*;

import com.provoly.common.item.CountDto;

public class ItemsSearchResult {

    private Map<ItemId, Item> items = new LinkedHashMap<>();
    private Collection<Relation> relations = new ArrayList<>();
    private SearchAfterContext searchAfter;
    private Map<ItemId, Item> sourceItems = new HashMap<>();
    private Map<ItemId, Item> destinationItems = new HashMap<>();

    private Map<UUID, CountDto> count;

    public void add(Item item) {
        items.put(item.getId(), item);
    }

    public List<Item> getSourceItems() {
        return new ArrayList<>(sourceItems.values());
    }

    public void addSource(Item item) {
        sourceItems.put(item.getId(), item);
    }

    public List<Item> getDestinationItems() {
        return new ArrayList<>(destinationItems.values());
    }

    public void addDestination(Item item) {
        destinationItems.put(item.getId(), item);
    }

    public List<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public Item getOne() {
        return items.values().iterator().next();
    }

    public Map<UUID, CountDto> getCount() {
        return count;
    }

    public void setCount(Map<UUID, CountDto> count) {
        this.count = count;
    }

    public void addRelation(String relationType, ItemId sourceId, ItemId destinationId) {
        relations.add(new Relation(relationType, sourceId, destinationId));
    }

    public Collection<Relation> getRelations() {
        return relations;
    }

    public SearchAfterContext getSearchAfter() {
        return searchAfter;
    }

    public void setSearchAfter(SearchAfterContext searchAfter) {
        this.searchAfter = searchAfter;
    }
}
