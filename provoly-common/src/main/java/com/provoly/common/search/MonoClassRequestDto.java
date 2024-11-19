package com.provoly.common.search;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class MonoClassRequestDto extends SearchRequestDto {
    private final UUID oClass;
    private final Collection<UUID> datasetVersionIds;
    private ConditionDto condition;
    private SortDto sort;
    private String searchAfter;

    @Default
    @JsonCreator
    public MonoClassRequestDto(SearchRequestType type, UUID oClass, Collection<UUID> datasetVersionIds, ConditionDto condition,
            SortDto sort, String searchAfter, boolean excludeGeo, int limit, Boolean withCount) {
        super(type, excludeGeo, limit, withCount == null || withCount);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
        this.condition = condition;
        this.sort = sort;
        this.searchAfter = searchAfter;
    }

    public MonoClassRequestDto(UUID oClass, ConditionDto condition, FullSearchConditionDto fullSearchConditionDto) {
        super(SearchRequestType.MONO_CLASS, fullSearchConditionDto);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = null;

    }

    public MonoClassRequestDto(UUID oClass, ConditionDto condition, FullSearchConditionDto fullSearchConditionDto, int limit) {
        super(SearchRequestType.MONO_CLASS, fullSearchConditionDto, limit);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = null;

    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, ConditionDto condition, int limit) {
        super(SearchRequestType.MONO_CLASS, false, limit);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, ConditionDto condition) {
        super(SearchRequestType.MONO_CLASS, false);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, ConditionDto condition, boolean excludeGeo) {
        super(SearchRequestType.MONO_CLASS, excludeGeo);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, ConditionDto condition,
            List<UUID> requestedAttributes, SortDto sort) {
        super(SearchRequestType.MONO_CLASS, false, requestedAttributes, false);
        this.oClass = oClass;
        this.condition = condition;
        this.datasetVersionIds = datasetVersionIds;
        this.sort = sort;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, FullSearchConditionDto fullSearchConditionDto) {
        super(SearchRequestType.MONO_CLASS, fullSearchConditionDto);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds) {
        super(SearchRequestType.MONO_CLASS, false);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, int limit) {
        super(SearchRequestType.MONO_CLASS, false, limit);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
    }

    public MonoClassRequestDto(UUID oClass, Collection<UUID> datasetVersionIds, boolean excludeGeo, int limit,
            ConditionDto condition) {
        super(SearchRequestType.MONO_CLASS, excludeGeo, limit, true);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
        this.condition = condition;
    }

    public MonoClassRequestDto(UUID oClass, List<UUID> datasetVersionIds, int limit) {
        super(SearchRequestType.MONO_CLASS, false, limit, true);
        this.oClass = oClass;
        this.datasetVersionIds = datasetVersionIds;
    }

    public String getSearchAfter() {
        return searchAfter;
    }

    public SortDto getSort() {
        return sort;
    }

    public ConditionDto getCondition() {
        return condition;
    }

    public void setCondition(ConditionDto condition) {
        this.condition = condition;
    }

    public Collection<UUID> getDatasetVersionIds() {
        return datasetVersionIds;
    }

    public UUID getoClass() {
        return oClass;
    }

    public void setSort(SortDto sort) {
        this.sort = sort;
    }

    public void setSearchAfter(String searchAfter) {
        this.searchAfter = searchAfter;
    }

    @Override
    public String toString() {
        return "{" +
                "type: \"" + getType() + '"' +
                ", oClass: \"" + oClass + '"' +
                ", datasetVersionIds:" + datasetVersionIds +
                ", condition:" + condition +
                ", searchAfter: \"" + searchAfter + '"' +
                ", sort:" + sort +
                "} ";
    }
}
