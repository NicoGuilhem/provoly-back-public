package com.provoly.replay;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;
import com.provoly.replay.entity.ErrorReport;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

@Path("/integrations/errors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorController {
    @Inject
    Logger log;

    @Inject
    ErrorService errorService;

    @GET
    @RolesAllowed({ Role.STR_INTEG_ERROR_READ })
    @Operation(summary = "Récupère la liste des classes et le nombre d'erreurs par type d'erreur", description = "Récupère les messages du flux kafka dans une map pour avoir les informations des erreurs mises à jour")
    public Collection<ErrorReport> getErrorReport() {
        return errorService.getReports();
    }

    @Path("/acknowledge/id/{id}")
    @POST
    @RolesAllowed({ Role.STR_INTEG_ERROR_WRITE })
    @Operation(summary = "Acquitte tous les items en erreur pour une classe donnée", description = " ")
    public void acquitError(
            @Parameter(in = ParameterIn.PATH, name = "id", description = "UUID de la classe à requêter") UUID id) {
        errorService.acquitRecords(id);
    }

    @Path("/acknowledge")
    @POST
    @RolesAllowed({ Role.STR_INTEG_ERROR_WRITE })
    @Operation(summary = "Acquitte tous les items en erreur pour une liste de classes données", description = " ")
    public void acquitErrors(
            @Parameter(in = ParameterIn.PATH, name = "id", description = "UUID de la classe à requêter") @RequestBody Set<UUID> classIds) {
        for (UUID id : classIds) {
            errorService.acquitRecords(id);
        }
    }

    @Path("/replay/class")
    @POST
    @RolesAllowed({ Role.STR_INTEG_ERROR_WRITE })
    @Operation(summary = "Reinsert dans la file d'entrée les items en erreur lors d'un précédent essai", description = "Recopie l'ensemble des items d'une classe donnée en erreur et qui n'ont pas été acquitées dans la file d'arrivé des nouveaux items. "
            +
            "Il seront alors pris en compte par le module de synchronisation pour une nouvelle tentative d'insertion dans la base")
    public void replay(
            @Parameter(in = ParameterIn.QUERY, name = "uuids", description = "List of uuids classes") @RequestBody Set<UUID> classIds) {
        errorService.replayRecord(classIds);

    }

}
