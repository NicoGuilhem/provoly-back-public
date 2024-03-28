package com.provoly.virt.dataset;

import java.util.Collection;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.item.ItemDto;

public record ImportRequest(DatasetDto dataset, DatasetVersionDto datasetVersion, Collection<ItemDto> items) {

}
