package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.exec.JobExecutionDetailsDto;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "provoly-exec-engine")
@Path("/job")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface ProvolyExecEngineService {

    @GET
    @Path("/executions/id/{id}")
    JobExecutionDetailsDto getJobExecution(UUID id);

}
