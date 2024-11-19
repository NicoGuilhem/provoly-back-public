package com.provoly.virt.item;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.DatasetVersionService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.search.MetadataConditionDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.common.search.Operator;
import com.provoly.common.search.OrConditionDto;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassSearchService;
import com.provoly.virt.storage.StorageRelationAdapters;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ReadItemsService {

    @Inject
    MonoClassSearchService searchService;

    @Inject
    @RestClient
    DatasetVersionService datasetVersionService;

    @Inject
    StorageRelationAdapters relationAdapters;

    public Item get(ItemId id) {
        return getOptional(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Item " + id + " not found"));
    }

    public List<Item> getList(List<ItemId> listOfIds) {
        Map<UUID, List<ItemId>> itemIdsByDatasetVersionId = listOfIds.stream()
                .collect(Collectors.groupingBy(ItemId::getDatasetVersionId));

        List<Item> itemListFound = new ArrayList<>();
        for (Map.Entry<UUID, List<ItemId>> entry : itemIdsByDatasetVersionId.entrySet()) {
            var dataset = datasetVersionService.get(entry.getKey());
            var listOfIdsCondition = entry.getValue().stream()
                    .map(id -> new MetadataConditionDto(MetadataSystem.ID, id.getAsString(), Operator.EQUALS))
                    .toList();
            OrConditionDto orCondition = new OrConditionDto();
            orCondition.composed.addAll(listOfIdsCondition);
            var request = new MonoClassRequestDto(dataset.getoClass(),
                    Collections.singletonList(dataset.getId()),
                    orCondition,
                    entry.getValue().size());
            ItemsSearchResult result = searchService.search(request);
            itemListFound.addAll(result.getItems());
        }
        return itemListFound;
    }

    public Optional<Item> getOptional(ItemId id) {
        var dataset = datasetVersionService.get(id.getDatasetVersionId());
        var request = new MonoClassRequestDto(dataset.getoClass(),
                Collections.singletonList(dataset.getId()),
                new MetadataConditionDto(MetadataSystem.ID, id.getAsString(), Operator.EQUALS),
                1);
        ItemsSearchResult result = searchService.search(request);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        if (result.size() > 1) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Multiple items :" + id);
        }

        var item = result.getOne();
        return Optional.of(item);
    }

    /**
     * Method to get all Relations and linked items using an ItemId.
     *
     * @param itemId item's id
     * @return itemSearchResult, response object which have all items and relations found
     */
    public ItemsSearchResult searchRelationsFromItem(final ItemId itemId) {
        Item targetItem = this.get(itemId);
        return relationAdapters.getRelationsByItem(targetItem);

    }
}
