package com.provoly.exec;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.exec.JobModelDto;
import com.provoly.common.user.Role;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/job/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobModelController {

    @Inject
    JobModelService jobModelService;

    @PUT
    @RolesAllowed({ Role.STR_JOB_MODEL_WRITE })
    @Operation(summary = "créé ou met à jour un nouveau model de travail", description = "Service permettant d'enregistrer un JobModelDto ou de le mettre à jour")
    public void save(JobModelDto jobModelDto) {
        jobModelService.save(jobModelDto);
    }

    @GET
    @RolesAllowed({ Role.STR_JOB_MODEL_READ })
    @Operation(summary = "récupérer la liste des modèles de travaux existant", description = "Service permettant de récuperer une collection de JobModelDTO")
    public Collection<JobModelDto> getAll() {
        return jobModelService.getAll();
    }

    @Path("/id/{id}")
    @GET
    @RolesAllowed({ Role.STR_JOB_MODEL_READ })
    @Operation(summary = "retoune un modèle de travaux", description = "Service permettant de retourner le jobModelDto en fonction de l'id. "
            +
            "Si le modèle de travail n'existe pas, retourne 404.")
    public JobModelDto getJobModel(UUID id) {
        return jobModelService.getJobModelById(id);
    }

    @Path("/id/{id}")
    @DELETE
    @RolesAllowed({ Role.STR_JOB_MODEL_WRITE })
    @Operation(summary = "Supprime un modèle de travail", description = "Service permettant de supprimer un JobModelDto en fonction de son id. "
            +
            "Si le modèle de travail n'existe pas, retourne 404.")
    public void deleteJobModel(UUID id) {
        jobModelService.deleteJobModelById(id);
    }

}
