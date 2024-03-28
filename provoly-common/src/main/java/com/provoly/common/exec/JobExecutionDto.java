package com.provoly.common.exec;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class JobExecutionDto {

    private final UUID id;
    private final UUID instance;

    @Default
    @JsonCreator
    public JobExecutionDto(UUID id, UUID instance) {
        this.id = id;
        this.instance = instance;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstance() {
        return instance;
    }
}
