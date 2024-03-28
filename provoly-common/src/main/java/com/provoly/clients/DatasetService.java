package com.provoly.clients;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/datasets")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface DatasetService {

    @GET
    @Path("/class/{id}")
    Collection<DatasetDto> getAllForClass(@PathParam("id") UUID id);

    @GET
    @Path("/id/{id}")
    @CacheResult(cacheName = "get-dataset-byID")
    DatasetDto get(@PathParam("id") UUID id);

    @PUT
    DatasetDto update(DatasetDto datasetDto);

    @POST
    DatasetDto save(DatasetDto datasetDto);

    @DELETE
    @Path("/id/{id}")
    void delete(@PathParam("id") UUID id);

    @GET
    @Path("/id/{id}/dataset-versions")
    List<DatasetVersionDto> getAllById(@PathParam("id") UUID id);

    @GET
    @Path("/search")
    @CacheResult(cacheName = "search-dataset-version")
    DatasetDto searchByDatasetVersionId(@RestQuery("dataset-version-id") UUID datasetVersionId);

    @GET
    @Path("/name/{datasetName}/dataset-version")
    DatasetVersionDto getDatasetVersionByDatasetName(@PathParam("datasetName") String datasetName);

    @GET
    @Path("/id/{datasetId}/dataset-version")
    DatasetVersionDto getDatasetVersionByDatasetId(@PathParam("datasetId") UUID datasetId);

}
