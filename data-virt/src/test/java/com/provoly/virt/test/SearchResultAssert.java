package com.provoly.virt.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.provoly.common.item.ItemDto;
import com.provoly.common.item.ItemsSearchResultDto;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

public class SearchResultAssert extends AbstractObjectAssert<SearchResultAssert, ItemsSearchResultDto> {

    protected SearchResultAssert(ItemsSearchResultDto actual) {
        super(actual, SearchResultAssert.class);
    }

    public static SearchResultAssert assertThat(ItemsSearchResultDto actual) {
        return new SearchResultAssert(actual);
    }

    public SearchResultAssert haveItemsForClass(UUID oClassId, ItemDto... items) {
        return checkItemsForClass(oClassId, ItemsSearchResultDto::items, items);
    }

    public SearchResultAssert haveSourceItemsForClass(UUID oClassId, ItemDto... items) {
        return checkItemsForClass(oClassId, ItemsSearchResultDto::sourceItems, items);
    }

    public SearchResultAssert haveDestinationItemsForClass(UUID oClassId, ItemDto... items) {
        return checkItemsForClass(oClassId, ItemsSearchResultDto::destinationItems, items);
    }

    private SearchResultAssert checkItemsForClass(UUID oClassId,
            Function<ItemsSearchResultDto, Map<UUID, List<ItemDto>>> itemsByClassExtractor, ItemDto... items) {
        var ids = Arrays.stream(items).map(ItemDto::getId).toList();
        var itemsForClassClass = itemsByClassExtractor.apply(actual).get(oClassId);
        Assertions.assertThat(itemsForClassClass).extracting(ItemDto::getId)
                .containsExactlyInAnyOrderElementsOf(ids);
        return this;
    }
}
