package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.dataset.DatasetVersionInformationDto;
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
    DatasetVersionDetailsDto get(@PathParam("id") UUID id);

    @POST
    void create(DatasetVersionDto datasetVersionDto);

    @POST
    @Path("/id/{id}/activate")
    void activate(@PathParam("id") UUID id);

    @PUT
    @Path("/id/{id}")
    void update(@PathParam("id") UUID id, DatasetVersionInformationDto datasetVersionInformationsDto);

    @POST
    @Path("/id/{id}/deactivate")
    void deactivate(UUID id);

    @DELETE
    @Path("/id/{id}")
    void delete(@PathParam("id") UUID id);

    @GET
    @Path("/class/{classId}")
    @CacheResult(cacheName = "dvs-all-active-for-class")
    Collection<DatasetVersionDetailsDto> getAllActiveForClass(@PathParam("classId") UUID classId);
}
