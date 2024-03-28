package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.exec.JobExecutionDto;
import com.provoly.exec.model.JobExecution;
import com.provoly.exec.model.JobMapper;

import org.jboss.logging.Logger;

@ApplicationScoped
public class JobExecutionService {

    @Inject
    Logger log;

    @Inject
    EntityIdService repo;

    @Inject
    JobMapper mapper;

    @Transactional
    public JobExecutionDetailsDto get(UUID id) {
        log.infof("Retrieve job execution %s", id);
        var jobExecution = repo.getById(id, JobExecution.class);
        return mapper.toDetailsDto(jobExecution);
    }

    public Collection<JobExecutionDetailsDto> getAll() {
        Collection<JobExecution> jobExecution = repo.getAll(JobExecution.class);
        return mapper.jobExecutionToDto(jobExecution);
    }

    @Transactional
    public void save(JobExecutionDto jobExecutionDto) {
        log.infof("Save jobExecution %s", jobExecutionDto);
        repo.persist(mapper.toEntity(jobExecutionDto));
    }

    public void delete(UUID id) {
        log.infof("deleting transfo with id %s", id);
        repo.removeEntity(id, JobExecution.class);
    }
}
