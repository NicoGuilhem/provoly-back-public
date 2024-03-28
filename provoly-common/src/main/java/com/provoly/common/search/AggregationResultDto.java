package com.provoly.common.search;

import java.util.List;

public record AggregationResultDto(AggregateOperation operation, List<ItemAggregationDto> values) {
}