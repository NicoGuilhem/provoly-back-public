package com.provoly.common.imports;

import java.util.Collection;

import com.provoly.common.dataset.DatasetVersionInformationDto;
import com.provoly.common.item.ItemDto;

public record ImportParameter(Collection<ItemDto> items, DatasetVersionInformationDto datasetVersionInformationDto) {
}
