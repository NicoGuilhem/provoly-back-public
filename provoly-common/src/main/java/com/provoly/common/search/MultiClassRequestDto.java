package com.provoly.common.search;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class MultiClassRequestDto extends SearchRequestDto {
    private MultiSearchType multiType;
    private final List<UUID> oClasses;
    private final Collection<FieldConditionDto> fields;

    @Default
    @JsonCreator
    public MultiClassRequestDto(MultiSearchType multiType, FullSearchConditionDto fullSearchConditionDto, List<UUID> oClasses,
            Collection<FieldConditionDto> fields, int limit) {
        super(SearchRequestType.MULTI_CLASS, fullSearchConditionDto, limit);
        this.multiType = multiType;
        this.oClasses = oClasses;
        this.fields = fields;
    }

    public MultiClassRequestDto(MultiSearchType multiType, List<UUID> oClasses, Collection<FieldConditionDto> fields,
            int limit) {
        this(multiType, null, oClasses, fields, limit);
    }

    public MultiClassRequestDto(MultiSearchType multiType, List<UUID> oClasses, Collection<FieldConditionDto> fields) {
        this(multiType, null, oClasses, fields, 0);
    }

    public MultiClassRequestDto(List<UUID> oClasses, Collection<FieldConditionDto> fields) {
        this(null, null, oClasses, fields, 0);
    }

    public MultiClassRequestDto(List<UUID> oClasses, Collection<FieldConditionDto> fields, int limit) {
        this(null, null, oClasses, fields, limit);
    }

    public MultiClassRequestDto(MultiSearchType multiType, FullSearchConditionDto fullSearchConditionDto) {
        this(multiType, fullSearchConditionDto, List.of(), List.of(), 0);
    }

    public MultiClassRequestDto(MultiSearchType multiType, FullSearchConditionDto fullSearchConditionDto, int limit) {
        this(multiType, fullSearchConditionDto, List.of(), List.of(), limit);
    }

    public MultiSearchType getMultiType() {
        return multiType;
    }

    public List<UUID> getoClasses() {
        return oClasses;
    }

    public Collection<FieldConditionDto> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "{" +
                "type: \"" + getType() + '"' +
                ", multiType: \"" + multiType + '"' +
                ", oClasses:" + oClasses +
                ", fields:" + fields +
                "} ";
    }
}
