package com.provoly.common.link;

import java.util.Objects;
import java.util.UUID;

import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.relation.RelationTypeDto;

public class LinkDetailsDto {

    private UUID id;
    private RelationTypeDto relationType;
    private AttributeDefDetailsDto attributeSource;
    private AttributeDefDetailsDto attributeDestination;

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

    public UUID getId() {
        return id;
    }

    public RelationTypeDto getRelationType() {
        return relationType;
    }

    public AttributeDefDetailsDto getAttributeSource() {
        return attributeSource;
    }

    public AttributeDefDetailsDto getAttributeDestination() {
        return attributeDestination;
    }

    public void setAttributeDestination(AttributeDefDetailsDto attributeDestination) {
        this.attributeDestination = attributeDestination;
    }

    public void setAttributeSource(AttributeDefDetailsDto attributeSource) {
        this.attributeSource = attributeSource;
    }
}
