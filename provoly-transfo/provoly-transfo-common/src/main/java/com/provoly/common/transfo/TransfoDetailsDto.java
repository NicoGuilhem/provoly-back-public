package com.provoly.common.transfo;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.provoly.common.exec.JobExecutionDetailsDto;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TransfoDetailsDto extends TransfoDto {
    private final Integer nodeNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private final Instant creationDate;

    private final UUID jobInstanceId;

    private final boolean active;

    private JobExecutionDetailsDto lastJobExecution;

    public TransfoDetailsDto(UUID id, Set<NodeDto> nodes, Set<LinkDto> links, String title, UUID jobInstanceId,
            String description, Instant creationDate, boolean active) {
        super(id, nodes, links, title, description);
        nodeNumber = nodes.size();
        this.creationDate = creationDate;
        this.active = active;
        this.jobInstanceId = jobInstanceId;
    }

    public Integer getNodeNumber() {
        return nodeNumber;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public boolean isActive() {
        return active;
    }

    public UUID getJobInstanceId() {
        return jobInstanceId;
    }

    public JobExecutionDetailsDto getLastJobExecution() {
        return lastJobExecution;
    }

    public void setLastJobExecution(JobExecutionDetailsDto lastJobExecution) {
        this.lastJobExecution = lastJobExecution;
    }
}
