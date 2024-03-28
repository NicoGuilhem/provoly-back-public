package com.provoly.transfo.engine;

import java.util.Collection;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.transfo.TransfoDetailsDto;
import com.provoly.common.transfo.TransfoDto;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_JSON)
@Path("/transfos")
public class TransfoController {

    @Inject
    TransfoService transfoService;

    @PUT
    public TransfoStatus saveAndValid(TransfoDto transfo) {
        return transfoService.saveAndValid(transfo);
    }

    @GET
    public Collection<TransfoDetailsDto> list() {
        return transfoService.getAll();
    }

    @GET
    @Path("/id/{id}")
    public TransfoDetailsDto get(UUID id) {
        return transfoService.get(id);
    }

    @DELETE
    @Path("/id/{id}")
    public void delete(UUID id) {
        transfoService.delete(id);
    }

    @POST
    @Path("/id/{id}/activation")
    public void activate(UUID id) {
        transfoService.activate(id);
    }

    @GET
    @Path("/id/{id}/status")
    public TransfoStatus getStatus(UUID id) {
        return transfoService.getStatus(id);
    }

    @DELETE
    @Path("/id/{id}/activation")
    public void deactivate(UUID id) {
        transfoService.deactivate(id);
    }
}
