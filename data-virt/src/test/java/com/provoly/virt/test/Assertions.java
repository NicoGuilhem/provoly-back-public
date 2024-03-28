package com.provoly.virt.test;

import com.provoly.common.item.ItemsSearchResultDto;

public class Assertions {

    public static SearchResultAssert assertThat(ItemsSearchResultDto actual) {
        return new SearchResultAssert(actual);
    }
}
