package com.provoly.ref.link;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.model.AttributeDef;
import com.provoly.ref.relation.RelationType;

@Entity
public class Link extends EntityId {

    @ManyToOne
    private RelationType relationType;

    @ManyToOne
    private AttributeDef attributeSource;

    @ManyToOne
    private AttributeDef attributeDestination;

    protected Link() {
        super();
    }

    @Default
    public Link(UUID id, RelationType relationType, AttributeDef attributeSource, AttributeDef attributeDestination) {
        super(id);
        this.relationType = relationType;
        this.attributeSource = attributeSource;
        this.attributeDestination = attributeDestination;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public AttributeDef getAttributeSource() {
        return attributeSource;
    }

    public AttributeDef getAttributeDestination() {
        return attributeDestination;
    }
}
