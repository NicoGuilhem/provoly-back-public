package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.dataset.DatasetVersionInformationsDto;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "data-ref")
@Path("/dataset-versions")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface DatasetVersionService {

    @GET
    @Path("/id/{id}")
    @CacheResult(cacheName = "get-dataset-version")
    DatasetVersionDto get(@PathParam("id") UUID id);

    @POST
    void create(DatasetVersionDto datasetVersionDto);

    @POST
    @Path("/id/{id}/activate")
    void activate(@PathParam("id") UUID id);

    @PUT
    void update(DatasetVersionInformationsDto datasetVersionInformationsDto);

    @POST
    @Path("/id/{id}/deactivate")
    void deactivate(UUID id);

    @DELETE
    @Path("/id/{id}")
    void delete(@PathParam("id") UUID id);

    @GET
    @Path("/class/{classId}")
    Collection<DatasetVersionDto> getAllActiveForClass(@PathParam("classId") UUID classId);
}
