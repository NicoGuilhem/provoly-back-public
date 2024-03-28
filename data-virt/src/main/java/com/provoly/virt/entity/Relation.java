package com.provoly.virt.entity;

import java.util.Objects;

public class Relation {

    /**
     * Slug of the relation type in ref database
     */
    private final String relationType;

    private final ItemId source;

    private final ItemId destination;

    /**
     * Only provisionned if it is a relation generated from a link
     */
    private final String aggregateId;

    public Relation(String relationType, ItemId source, ItemId destination) {
        this(relationType, source, destination, null);
    }

    public Relation(String relationType, ItemId source, ItemId destination, String aggregateId) {
        this.relationType = relationType;
        this.source = source;
        this.destination = destination;
        this.aggregateId = aggregateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Relation relation = (Relation) o;
        return relationType.equals(relation.relationType) && source.equals(relation.source)
                && destination.equals(relation.destination) && Objects.equals(aggregateId, relation.aggregateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationType, source, destination, aggregateId);
    }

    public String getRelationType() {
        return relationType;
    }

    public ItemId getSource() {
        return source;
    }

    public ItemId getDestination() {
        return destination;
    }

    public String getAggregateId() {
        return aggregateId;
    }
}
