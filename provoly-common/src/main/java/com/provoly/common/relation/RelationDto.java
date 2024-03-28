package com.provoly.common.relation;

public class RelationDto {
    private final String relationType;
    private final String source;
    private final String destination;

    public RelationDto(String relationType, String source, String destination) {
        this.relationType = relationType;
        this.source = source;
        this.destination = destination;
    }

    public String getRelationType() {
        return relationType;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

}
