package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Provide datasource details from a dataset, a dataset definition or a request
 */
@Path("/data-sources")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface DataSourceService {

    @GET
    @Path("/id/{dataSourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    DataSourceDetailsDto getDataSourceDetails(UUID dataSourceId);
}
