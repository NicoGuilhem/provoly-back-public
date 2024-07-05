package com.provoly.clients;

import java.util.Collection;
import java.util.UUID;

import jakarta.ws.rs.*;

import com.provoly.common.error.ProvolyResponseExceptionMapper;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.OClassReadDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;

import io.quarkus.cache.CacheResult;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/model")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(ProvolyAuthentRequestFilter.class)
public interface ModelService {

    @GET
    @Path("/fields/class/{id}")
    @CacheResult(cacheName = "fields-for-class")
    Collection<FieldDto> getFieldsForClass(@PathParam("id") UUID id);

    @GET
    @Path("/fields/{id}")
    @CacheResult(cacheName = "field-by-id")
    FieldDto getFieldById(@PathParam("id") UUID id);

    @GET
    @Path("/class/id/{id}")
    @CacheResult(cacheName = "class-dto")
    OClassReadDto get(@PathParam("id") UUID id);

    @GET
    @Path("/class/id/{id}/details")
    @CacheResult(cacheName = "class-dto-details")
    OClassDetailsDto getDetails(@PathParam("id") UUID id);

    @POST
    @Path("/class")
    void addClass(OClassWriteDto oclassDto);

    @GET
    @Path("/class")
    Collection<OClassReadDto> getAllClasses();

    @DELETE
    @Path("/class/id/{id}")
    void deleteClass(@PathParam("id") UUID id);

    @POST
    @Path("/fields")
    void addField(FieldDto fieldDto);

    @PUT
    @Path("/fields/id/{id}")
    void updateField(@PathParam("id") UUID id, FieldDto fieldDto);

    @GET
    @Path("/fields")
    public Collection<FieldDto> getFields();

    @DELETE
    @Path("/fields/{id}")
    public void deleteFieldById(UUID id);
}
