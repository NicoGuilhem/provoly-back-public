package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.link.LinkDetailsDto;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface LinkService {

    @GET
    @CacheResult(cacheName = "link")
    @Path("/links/id/{id}")
    LinkDetailsDto getById(@PathParam("id") UUID id);

    @GET
    @Path("/links")
    Collection<LinkDetailsDto> getAllLinks();

}
