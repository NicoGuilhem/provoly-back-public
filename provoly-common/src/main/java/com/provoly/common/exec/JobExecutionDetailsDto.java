package com.provoly.common.exec;

import java.time.Instant;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class JobExecutionDetailsDto {
    private final UUID id;
    private final JobInstanceDetailsDto instance;
    private ExecutionStatus status;
    private Instant executionDate;

    @Default
    @JsonCreator
    public JobExecutionDetailsDto(UUID id, JobInstanceDetailsDto instance, ExecutionStatus status, Instant executionDate) {
        this.id = id;
        this.instance = instance;
        this.status = status;
        this.executionDate = executionDate;
    }

    public UUID getId() {
        return id;
    }

    public JobInstanceDetailsDto getInstance() {
        return instance;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Instant getExecutionDate() {
        return executionDate;
    }

}
