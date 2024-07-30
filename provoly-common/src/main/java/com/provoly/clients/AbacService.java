package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.abac.ContextVariableDto;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/abac")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface AbacService {

    @GET
    @Path("/rules/class/{oClassId}")
    @CacheResult(cacheName = "get-rules-for-class")
    Collection<AbacRuleDto> getRuleFor(@PathParam("oClassId") UUID oClassId);

    @GET
    @Path("/rules")
    Collection<AbacRuleDto> getAllRules(@RestQuery("type") AbacRuleType type);

    @POST
    @Path("/rules")
    void addRule(AbacRuleDto rule);

    @DELETE
    @Path("/rules/{ruleId}")
    void deleteRule(@PathParam("ruleId") UUID ruleId);

    @GET
    @Path("/context")
    @CacheResult(cacheName = "context-variables")
    Collection<ContextVariableDto> listContextVariables();

    @GET
    @Path("/context/{variableName}")
    ContextVariableDto getContextVariable(@PathParam("variableName") String variableName);
}
