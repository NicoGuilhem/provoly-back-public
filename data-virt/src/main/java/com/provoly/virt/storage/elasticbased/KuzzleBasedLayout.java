package com.provoly.virt.storage.elasticbased;

import java.util.Map;
import java.util.UUID;

import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.ComposedConditionDto;
import com.provoly.virt.entity.Item;

public abstract class KuzzleBasedLayout extends StorageLayout {
    public abstract ComposedConditionDto getLayoutConditions(OClassDetailsDto classDto);

    public abstract Item convertToItem(Map<String, Object> hit, OClassDetailsDto oClass, UUID datasetversion);
}
