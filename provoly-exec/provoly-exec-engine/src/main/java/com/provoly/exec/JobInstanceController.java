package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.exec.JobInstanceDetailsDto;
import com.provoly.common.exec.JobInstanceDto;
import com.provoly.common.user.Role;

@Path("/job/instances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobInstanceController {

    @Inject
    JobInstanceService jobInstanceService;

    @Inject
    JobService jobService;

    @POST
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    public void save(JobInstanceDto jobInstanceDto) {
        jobInstanceService.save(jobInstanceDto);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_INSTANCE_READ })
    public Collection<JobInstanceDetailsDto> getAll() {
        return jobInstanceService.getAllInstances();
    }

    @GET
    @Path("/id/{jobInstanceUuid}")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_READ })
    public JobInstanceDetailsDto get(UUID jobInstanceUuid) {
        return jobInstanceService.getInstance(jobInstanceUuid);
    }

    @GET
    @Path("/id/{jobInstanceUuid}/execution")
    @RolesAllowed({ Role.STR_JOB_EXECUTION_READ })
    public JobExecutionDetailsDto getLastJobExecution(UUID jobInstanceUuid) {
        return jobService.getLastJobExecutionForJobInstance(jobInstanceUuid);
    }

    @PUT
    @Path("/id/{jobInstanceUuid}/start")
    @RolesAllowed({ Role.STR_JOB_START })
    public JobExecutionDetailsDto start(UUID jobInstanceUuid) {
        return jobService.start(jobInstanceUuid);
    }

    @PUT
    @Path("/id/{id}/activation")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    public void activate(UUID id) {
        jobInstanceService.activate(id);
    }

    @DELETE
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    public void deleteById(UUID id) {
        jobInstanceService.deleteById(id);
    }

    @DELETE
    @Path("/id/{id}/activation")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    public void deactivate(UUID jobInstanceUuid) {
        jobInstanceService.deactivate(jobInstanceUuid);
    }

    // Mainly used by tests to enforce executionId :(
    public JobExecutionDetailsDto start(UUID jobExecutionId, UUID jobInstanceUuid) {
        return jobService.start(jobExecutionId, jobInstanceUuid);
    }

}
