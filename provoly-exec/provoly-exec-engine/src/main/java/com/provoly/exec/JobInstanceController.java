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

import org.eclipse.microprofile.openapi.annotations.Operation;

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
    @Operation(summary = "Créé ou met à jour une nouvelle instance d'un travail.", description = "Service permettant d'enregistrer une nouvelle instance d'un travail (JobInstance) ou de la mettre à jour.")
    public void save(JobInstanceDto jobInstanceDto) {
        jobInstanceService.save(jobInstanceDto);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_INSTANCE_READ })
    @Operation(summary = "Renvoie l'ensemble des JobInstance existant", description = "Service permettant de récupérer une collection de JobInstanceDTO, si aucune instance existant le contenu retourné est vide.")
    public Collection<JobInstanceDetailsDto> getAll() {
        return jobInstanceService.getAllInstances();
    }

    @GET
    @Path("/id/{jobInstanceUuid}")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_READ })
    @Operation(summary = "Renvoie le jobInstance correspondant à l'id fournie.", description = "Le jobInstance possédant l'ID fournie est renvoyé à l'utilisateur, si aucun jobInstance ne correspond à l'ID une erreur 404 est levé")
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
    @Operation(summary = "Exécute une instance de travail (jobInstance) existante.", description = "Exécute le jobInstance pointé par l'ID et renvoie une instance d'exécution à l'utilisateur.")
    public JobExecutionDetailsDto start(UUID jobInstanceUuid) {
        return jobService.start(jobInstanceUuid);
    }

    @PUT
    @Path("/id/{id}/activation")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    @Operation(summary = "Activating a job instance by id", description = "Activating a job instance by set activate attribute TRUE, if already activated return 409")
    public void activate(UUID id) {
        jobInstanceService.activate(id);
    }

    @DELETE
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    @Operation(summary = "Supprime l'instance correspondant à l'ID envoyée", description = "Suppression du jobInstance correspondant à l'ID dans l'URL, si n'existe pas une 404 est levé")
    public void deleteById(UUID id) {
        jobInstanceService.deleteById(id);
    }

    @DELETE
    @Path("/id/{id}/activation")
    @RolesAllowed({ Role.STR_JOB_INSTANCE_WRITE })
    @Operation(summary = "Deactivating a job instance by his ID.", description = "Deactivating a job instance by set activate attribute FALSE, if already deactivated return 409.")
    public void deactivate(UUID id) {
        jobInstanceService.deactivate(id);
    }

    // Mainly used by tests to enforce executionId :(
    public JobExecutionDetailsDto start(UUID jobExecutionId, UUID jobInstanceUuid) {
        return jobService.start(jobExecutionId, jobInstanceUuid);
    }

}
