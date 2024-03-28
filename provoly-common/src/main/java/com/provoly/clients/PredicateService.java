package com.provoly.clients;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.provoly.common.abac.PredicateDto;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/predicates")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface PredicateService {

    @GET
    @Path("/id/{id}")
    @CacheResult(cacheName = "get-predicate")
    PredicateDto getPredicate(@PathParam("id") UUID id);

    @POST
    void savePredicate(PredicateDto pred);
}
