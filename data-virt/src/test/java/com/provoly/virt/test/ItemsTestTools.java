package com.provoly.virt.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.relation.RelationDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.search.*;
import com.provoly.test.AuthService;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.item.ItemsController;
import com.provoly.virt.metadata.MetadataController;
import com.provoly.virt.relation.RelationsController;
import com.provoly.virt.search.SearchController;

@ApplicationScoped
public class ItemsTestTools {

    @Inject
    AuthService authService;

    @Inject
    ItemsController itemsController;

    @Inject
    SearchController searchController;

    @Inject
    MetadataController metadataController;

    @Inject
    RelationsController relationsController;

    public void addMetadataToItem(ItemDto item, MetadataDefDto metadata, String value) {
        metadataController.setMetadataToItem(new ItemId(item.getId()), metadata.name, value);
    }

    public void addMetadataToAttribute(ItemDto item, AttributeDefDto attribute, MetadataDefDto metadata, String value) {
        metadataController.setMetadataToAttribute(new ItemId(item.getId()), attribute.getName(), metadata.name, value);
    }

    public ItemsSearchResultDto searchAll(UUID oClass, DatasetVersionDto dataset) {
        return searchAll(oClass, dataset, (SortDto) null);
    }

    public ItemsSearchResultDto searchAll(UUID oClass, DatasetVersionDto dataset, ConditionDto condition) {
        return searchAll(oClass, dataset, condition, 10);
    }

    public ItemsSearchResultDto searchAll(UUID oClass, DatasetVersionDto dataset, ConditionDto condition, int limit) {
        var request = new MonoClassRequestDto(oClass, Collections.singleton(dataset.getId()), condition, limit);
        return searchController.search(request, null);
    }

    public ItemsSearchResultDto searchAll(UUID oClass, DatasetVersionDto dataset, ConditionDto condition, boolean excludeGeo) {
        var request = new MonoClassRequestDto(oClass, Collections.singleton(dataset.getId()), condition, excludeGeo);
        return searchController.search(request, null);
    }

    public ItemsSearchResultDto searchAll(UUID oClass, DatasetVersionDto dataset, SortDto sort) {
        var request = new MonoClassRequestDto(oClass, Collections.singleton(dataset.getId()));
        return searchController.search(request, sort);
    }

    public ItemsSearchResultDto searchMulti(MultiClassRequestDto condition) {
        return searchController.search(condition, null);
    }

    public ItemsSearchResultDto searchFull(UUID oClassId, DatasetVersionDto dataset, FullSearchConditionDto fullSearch) {
        var request = new MonoClassRequestDto(oClassId, Collections.singleton(dataset.getId()), fullSearch);
        return searchController.search(request, null);
    }

    public ItemsSearchResultDto searchFull(FullSearchConditionDto fullSearch) {
        var request = new MultiClassRequestDto(MultiSearchType.AND, fullSearch);
        return searchController.search(request, null);
    }

    public ItemsSearchResultDto searchByNamedQuery(UUID namedQueryId) {
        return searchController.searchByNamedQuery(namedQueryId, null, 0);
    }

    public ItemDto searchItem(String itemId) {
        return itemsController.get(new ItemId(itemId));
    }

    public void checkItemExists(String itemId) {
        var item = searchItem(itemId);
        assertThat(itemId).isEqualTo(item.getId());
    }

    public String addItem(DatasetVersionDto dataset) {
        UUID itemId = UUID.randomUUID();
        var item = new ItemDto(dataset, itemId.toString());
        item.put("Num_Acc", "200000000001L");
        authService.authenticate();
        itemsController.insertOrUpdate(List.of(item), null);
        return item.getId();
    }

    public ItemDto addItem(DatasetVersionDto datasetVersion, Map<String, Object> attributes) {
        UUID itemId = UUID.randomUUID();
        var item = new ItemDto(datasetVersion, itemId.toString());
        attributes.forEach(item::put);

        itemsController.insertOrUpdate(List.of(item), null);

        return item;
    }

    public void createRelation(RelationTypeDto relationType, ItemDto source, ItemDto destination) {
        var relation = new RelationDto(relationType.slug, source.getId(), destination.getId());
        relationsController.saveRelations(Collections.singleton(relation));
    }

}