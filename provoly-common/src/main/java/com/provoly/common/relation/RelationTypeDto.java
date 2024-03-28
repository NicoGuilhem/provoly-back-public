package com.provoly.common.relation;

import java.util.UUID;

public class RelationTypeDto {
    public UUID id;
    public String name;
    public String slug;

    public RelationTypeDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
}
