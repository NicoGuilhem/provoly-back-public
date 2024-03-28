package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.exec.JobInstanceDetailsDto;
import com.provoly.common.exec.JobInstanceDto;
import com.provoly.exec.model.JobInstance;
import com.provoly.exec.model.JobMapper;

import org.jboss.logging.Logger;

@ApplicationScoped
public class JobInstanceService {

    @Inject
    Logger log;

    @Inject
    EntityIdService repo;

    @Inject
    JobMapper mapper;

    // TODO : Remove duplication
    // TODO : We can update an instance only if it has never been started
    //  => Save all executions
    //  => Manage instances versions
    // TODO : Check parameters are present in model
    @Transactional
    public void save(JobInstanceDto jobInstanceDto) {
        log.infof("Save jobInstance %s", jobInstanceDto);
        var entity = repo.findById(jobInstanceDto.getId(), JobInstance.class);
        entity.ifPresentOrElse(
                e -> mapper.update(jobInstanceDto, e),
                () -> repo.persist(mapper.toEntity(jobInstanceDto)));
    }

    @Transactional
    public JobInstanceDetailsDto getInstance(UUID id) {
        log.infof("Retrieve job instance %s", id);
        var jobInstance = repo.getById(id, JobInstance.class);
        return mapper.toDetailsDto(jobInstance);
    }

    public Collection<JobInstanceDetailsDto> getAllInstances() {
        log.infof("Retrieve all job instances");
        Collection<JobInstance> jobInstanceCollection = repo.getAll(JobInstance.class);
        return mapper.toJobInstanceDetailsDtoCollection(jobInstanceCollection);
    }

    public void deleteById(UUID id) {
        log.infof("Delete job instance with id: %s", id);
        repo.removeEntity(id, JobInstance.class);
    }

    @Transactional
    public void activate(UUID id) {
        log.infof("Activating job instance with id: %s", id);
        JobInstance jobInstance = repo.getById(id, JobInstance.class);
        if (jobInstance.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Job instance is already active");
        }
        jobInstance.setActive(true);
    }

    @Transactional
    public void deactivate(UUID id) {
        log.infof("Deactivating job instance with id: %s", id);
        JobInstance jobInstance = repo.getById(id, JobInstance.class);

        if (!jobInstance.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Job instance is already inactive");
        }
        jobInstance.setActive(false);
    }
}
