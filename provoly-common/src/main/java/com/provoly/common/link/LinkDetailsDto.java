package com.provoly.common.link;

import java.util.Objects;
import java.util.UUID;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.relation.RelationTypeDto;

public class LinkDetailsDto {

    public UUID id;
    public RelationTypeDto relationType;
    public AttributeDefDetailsDto attributeSource;
    public AttributeDefDetailsDto attributeDestination;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LinkDetailsDto linkDto = (LinkDetailsDto) o;
        return id.equals(linkDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LinkDetailsDto{id=" + id + ", " + attributeSource + "->" + attributeDestination + '}';
    }
}
