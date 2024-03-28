package com.provoly.virt.test;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

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
        var ids = Arrays.stream(items).map(ItemDto::getId).collect(Collectors.toList());
        Assertions.assertThat(actual.items().get(oClassId)).extracting(i -> i.getId())
                .containsExactlyInAnyOrderElementsOf(ids);
        return this;
    }

}
