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

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/job/executions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobExecutionController {

    @Inject
    JobExecutionService jobExecutionService;

    @GET
    @RolesAllowed({ Role.STR_JOB_EXECUTION_READ })
    @Operation(summary = "retourne une exécution de travail", description = "Service permettant de retourner le JobExecution en fonction de l'id. "
            +
            "Si l'exécution de travail n'existe pas, retourne 404.")
    @Path("/id/{id}")
    public JobExecutionDetailsDto get(UUID id) {
        return jobExecutionService.get(id);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_EXECUTION_READ })
    @Operation(summary = "retourne la liste des exécutions de travail", description = "Service permettant de retourner la liste des JobExecution. "
            +
            "Si il n'y a pas d'exécution de travail dans la liste, renvoie un tableau vide.")
    public Collection<JobExecutionDetailsDto> getAll() {
        return jobExecutionService.getAll();
    }

}
