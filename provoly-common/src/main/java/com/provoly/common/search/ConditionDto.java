package com.provoly.common.search;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrueConditionDto.class, name = "TRUE"),
        @JsonSubTypes.Type(value = AndConditionDto.class, name = "AND"),
        @JsonSubTypes.Type(value = OrConditionDto.class, name = "OR"),
        @JsonSubTypes.Type(value = AttributeConditionDto.class, name = "ATTRIBUTE"),
        @JsonSubTypes.Type(value = MetadataConditionDto.class, name = "METADATA")
})
public abstract class ConditionDto {

    public ConditionType type;

}
