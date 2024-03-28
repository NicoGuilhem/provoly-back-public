package com.provoly.common.relation;

import java.time.Instant;
import java.util.UUID;

public class RelationTypeDetailsDto extends RelationTypeDto {
    public int nbRelation;
    public int nbLink;
    public boolean deletable;
    public Instant modificationDate;

    public RelationTypeDetailsDto(UUID id, String name, int nbRelation, int nbLink, boolean deletable,
            Instant modificationDate) {
        super(id, name);
        this.nbRelation = nbRelation;
        this.nbLink = nbLink;
        this.deletable = deletable;
        this.modificationDate = modificationDate;
    }
}
