package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.metadata.MetadataDefDto;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/metadata")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface MetadataRefService {

    @GET
    @Path("/id/{metadataId}")
    @CacheResult(cacheName = "metadata-by-id")
    MetadataDefDto get(@PathParam("metadataId") UUID metadataId);

    @GET
    @Path("/name/{metadataName}")
    @CacheResult(cacheName = "metadata-by-name")
    MetadataDefDto getByName(@PathParam("metadataName") String metadataName);

    @GET
    @Path("/slug/{metadataSlug}")
    @CacheResult(cacheName = "metadata-by-slug")
    MetadataDefDto getBySlug(@PathParam("metadataSlug") String metadataSlug);

    @POST
    void saveMetadataDef(MetadataDefDto metadataDef);
}
