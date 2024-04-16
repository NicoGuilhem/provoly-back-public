package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.exec.JobModelDto;
import com.provoly.common.user.Role;

@Path("/job/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobModelController {

    @Inject
    JobModelService jobModelService;

    @PUT
    @RolesAllowed({ Role.STR_JOB_MODEL_WRITE })
    public void save(JobModelDto jobModelDto) {
        jobModelService.save(jobModelDto);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_MODEL_READ })
    public Collection<JobModelDto> getAll() {
        return jobModelService.getAll();
    }

    @Path("/id/{id}")
    @GET
    @RolesAllowed({ Role.STR_JOB_MODEL_READ })
    public JobModelDto getJobModel(UUID id) {
        return jobModelService.getJobModelById(id);
    }

    @Path("/id/{id}")
    @DELETE
    @RolesAllowed({ Role.STR_JOB_MODEL_WRITE })
    public void deleteJobModel(UUID id) {
        jobModelService.deleteJobModelById(id);
    }

}
