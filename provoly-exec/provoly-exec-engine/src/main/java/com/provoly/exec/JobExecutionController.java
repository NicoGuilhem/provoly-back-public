package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.user.Role;

@Path("/job/executions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobExecutionController {

    @Inject
    JobExecutionService jobExecutionService;

    @GET
    @RolesAllowed({ Role.STR_JOB_EXECUTION_READ })
    @Path("/id/{id}")
    public JobExecutionDetailsDto get(UUID id) {
        return jobExecutionService.get(id);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_EXECUTION_READ })
    public Collection<JobExecutionDetailsDto> getAll() {
        return jobExecutionService.getAll();
    }

}
