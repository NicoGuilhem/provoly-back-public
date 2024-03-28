package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.exec.JobModelDto;
import com.provoly.exec.model.JobMapper;
import com.provoly.exec.model.JobModel;

import org.jboss.logging.Logger;

@ApplicationScoped
public class JobModelService {

    @Inject
    Logger log;

    @Inject
    EntityIdService repo;

    @Inject
    JobMapper mapper;

    // TODO : Check filename not contain "/" or other illegal characters for a filename
    @Transactional
    public void save(JobModelDto jobModelDto) {
        log.infof("Save jobModel %s", jobModelDto);
        var entity = repo.findById(jobModelDto.getId(), JobModel.class);
        entity.ifPresentOrElse(
                e -> mapper.update(jobModelDto, e),
                () -> repo.persist(mapper.toEntity(jobModelDto)));
    }

    public Collection<JobModelDto> getAll() {
        return mapper.toCollectionDto(repo.getAll(JobModel.class));
    }

    @Transactional
    public JobModelDto getJobModelById(UUID id) {
        return mapper.toDto(repo.getById(id, JobModel.class));
    }

    public void deleteJobModelById(UUID id) {
        repo.removeEntity(id, JobModel.class);
    }
}
