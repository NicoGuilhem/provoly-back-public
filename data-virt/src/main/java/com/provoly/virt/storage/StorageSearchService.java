package com.provoly.virt.storage;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassContextRequest;

public interface StorageSearchService {

    ItemsSearchResult search(OClassDetailsDto classDto,
            MonoClassRequestDto request,
            MonoClassContextRequest monoClassContextRequest);

}
