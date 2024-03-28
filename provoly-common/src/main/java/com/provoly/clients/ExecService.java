package com.provoly.clients;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.exec.JobExecutionDetailsDto;
import com.provoly.common.exec.JobInstanceDetailsDto;
import com.provoly.common.exec.JobInstanceDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_JSON)
@Path("/job/instances")
@RegisterRestClient(configKey = "provoly-exec-engine")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
@ApplicationScoped
public interface ExecService {
    @POST
    void createInstance(JobInstanceDto jobInstanceDto);

    @GET
    @Path("/id/{jobInstanceId}")
    JobInstanceDetailsDto get(UUID jobInstanceId);

    @GET
    @Path("/{jobInstanceUuid}/execution")
    JobExecutionDetailsDto getLastJobExecution(UUID jobInstanceUuid);

    @DELETE
    @Path("/id/{id}/activation")
    void deactivate(UUID id);

    @PUT
    @Path("/id/{id}/activation")
    void activate(UUID id);

    @PUT
    @Path("/id/{jobInstanceUuid}/start")
    JobExecutionDetailsDto start(UUID jobInstanceUuid);

}