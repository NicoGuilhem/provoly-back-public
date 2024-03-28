package com.provoly.common.item;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.relation.RelationDto;

public record ItemsSearchResultDto(
        Map<UUID, List<ItemDto>> items,
        Collection<RelationDto> relations,
        Map<UUID, CountDto> count,
        String searchAfter,
        GeoFormat geoFormat) {

    public ItemsSearchResultDto(Map<UUID, List<ItemDto>> items, Collection<RelationDto> relations, Map<UUID, CountDto> count) {
        this(items, relations, count, "", GeoFormat.GEO_JSON);
    }

    public ItemsSearchResultDto(Map<UUID, List<ItemDto>> items, Collection<RelationDto> relations, Map<UUID, CountDto> count,
            String searchAfter) {
        this(items, relations, count, searchAfter, GeoFormat.GEO_JSON);
    }

    public ItemsSearchResultDto(ItemsSearchResultDto result, GeoFormat geoFormat) {
        this(result.items, result.relations, result.count, result.searchAfter, geoFormat);
    }
}
