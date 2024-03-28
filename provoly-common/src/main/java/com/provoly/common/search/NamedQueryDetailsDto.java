package com.provoly.common.search;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class NamedQueryDetailsDto extends NamedQueryDto {

    private final Instant lastExecutionDate;
    private final boolean owner;

    @Default
    @JsonCreator
    public NamedQueryDetailsDto(Instant lastExecutionDate, boolean owner, UUID id, String name, String description,
            SearchRequestDto request, boolean favorite, String color, VisibilityDto visibility) {
        super(id, name, description, request, favorite, color, visibility);
        this.lastExecutionDate = lastExecutionDate;
        this.owner = owner;
    }

    public NamedQueryDetailsDto(UUID id, String name, String description, SearchRequestDto request, VisibilityDto visibility) {
        super(id, name, description, request, visibility);
        this.lastExecutionDate = null;
        this.owner = false;
    }

    public Instant getLastExecutionDate() {
        return lastExecutionDate;
    }

    public boolean isOwner() {
        return owner;
    }
}
