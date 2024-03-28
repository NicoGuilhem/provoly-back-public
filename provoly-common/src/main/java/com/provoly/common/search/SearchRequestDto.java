package com.provoly.common.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.item.GeoFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MonoClassRequestDto.class, name = "MONO_CLASS"),
        @JsonSubTypes.Type(value = MultiClassRequestDto.class, name = "MULTI_CLASS"),
})
public abstract class SearchRequestDto {
    private final UUID id;
    private final SearchRequestType type;
    private final FullSearchConditionDto fullSearch;
    private boolean excludeGeo;
    private int limit;

    private boolean withCount;
    private List<UUID> requestedAttributes;
    private GeoFormat geoFormat;

    protected SearchRequestDto(SearchRequestType type, boolean excludeGeo) {
        this(null, type, new ArrayList<>(), null, excludeGeo, 0, true, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(SearchRequestType type) {
        this(null, type, new ArrayList<>(), null, false, 0, true, GeoFormat.GEO_JSON);
    }

    @Default
    @JsonCreator
    protected SearchRequestDto(SearchRequestType type, boolean excludeGeo, boolean withCount, GeoFormat geoFormat) {
        this(null, type, new ArrayList<>(), null, excludeGeo, 0, withCount, geoFormat);
    }

    protected SearchRequestDto(SearchRequestType type, boolean excludeGeo, int limit) {
        this(null, type, new ArrayList<>(), null, excludeGeo, limit, true, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(SearchRequestType type, boolean excludeGeo, List<UUID> requestedAttributes,
            boolean withCount) {
        this(null, type, requestedAttributes, null, excludeGeo, 0, withCount, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(SearchRequestType type, FullSearchConditionDto fullSearch) {
        this(null, type, new ArrayList<>(), fullSearch, false, 0, true, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(SearchRequestType type, FullSearchConditionDto fullSearch, int limit) {
        this(null, type, new ArrayList<>(), fullSearch, false, limit, true, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(FullSearchConditionDto fullSearch) {
        this(null, null, new ArrayList<>(), fullSearch, false, 0, true, GeoFormat.GEO_JSON);
    }

    protected SearchRequestDto(UUID id, SearchRequestType type, List<UUID> requestedAttributes,
            FullSearchConditionDto fullSearch, boolean excludeGeo, int limit, boolean withCount, GeoFormat geoFormat) {
        this.id = id;
        this.type = type;
        this.fullSearch = fullSearch;
        this.excludeGeo = excludeGeo;
        this.withCount = withCount;
        this.requestedAttributes = requestedAttributes;
        this.geoFormat = geoFormat;
        this.limit = limit;
    }

    protected SearchRequestDto(SearchRequestType type, boolean excludeGeo, int limit, boolean withCount) {
        this(null, type, new ArrayList<>(), null, excludeGeo, limit, withCount, GeoFormat.GEO_JSON);
    }

    public SearchRequestType getType() {
        return type;
    }

    public FullSearchConditionDto getFullSearch() {
        return fullSearch;
    }

    public UUID getId() {
        return id;
    }

    public boolean isExcludeGeo() {
        return excludeGeo;
    }

    public void setExcludeGeo(boolean excludeGeo) {
        this.excludeGeo = excludeGeo;
    }

    public GeoFormat getGeoFormat() {
        return this.geoFormat;
    }

    public List<UUID> getRequestedAttributes() {
        return requestedAttributes == null ? new ArrayList<>() : requestedAttributes;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isWithCount() {
        return withCount;
    }
}
