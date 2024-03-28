package com.provoly.virt;

import java.util.Map;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "kuzzle-device-manager")
public interface KuzzleDeviceManagerClient {

    @POST
    @Path("/_/device-manager/models/measures")
    void createMeasureModel(Map<String, Object> measureModel);

    @POST
    @Path("/_/device-manager/models/assets")
    void createAssetModel(Map<String, Object> assetModel);

    @POST
    @Path("/_/device-manager/models/devices")
    void createDeviceModel(Map<String, Object> deviceModel);

    @POST
    @Path("/_/device-manager/engine/{name}")
    void createEngine(@PathParam("name") String name);

    @GET
    @Path("/_/device-manager/engine/{name}/_exists")
    Response getEngine(@PathParam("name") String name);

    @POST
    @Path("/_/device-manager/{name}/devices")
    void createDevice(@PathParam("name") String name, Map<String, Object> device);

    @DELETE
    @Path("device-manager/devices/{deviceId}")
    void deleteDevice(@PathParam("deviceId") String deviceId);

    @GET
    @Path("device-manager/devices/{deviceId}")
    Response getDevice(@PathParam("deviceId") String deviceId);

    @POST
    @Path("/_/device-manager/{name}/assets")
    Response createAsset(@PathParam("name") String name, Map<String, Object> asset);

    @GET
    @Path("/_/device-manager/{name}/assets/{assetId}")
    Response getAsset(@PathParam("name") String name, @PathParam("assetId") String assetId);

    @PUT
    @Path("/_/device-manager/{name}/devices/{deviceId}/_link/{assetId}")
    void linkDeviceToAsset(@PathParam("name") String name, @PathParam("deviceId") String deviceId,
            @PathParam("assetId") String assetId, Map<String, Object> measureToLink);

    @PUT
    @Path("/_/device-manager/{name}/devices/{deviceId}/measures")
    void receiveMeasure(@PathParam("name") String name, @PathParam("deviceId") String deviceId, Map<String, Object> measure);

    @GET
    @Path("/_/device-manager/{name}/devices/{deviceId}/measures")
    RestResponse<Map<String, Object>> getMeasuresForDevice(@PathParam("name") String name,
            @PathParam("deviceId") String deviceId);

    @DELETE
    @Path("/_/device-manager/engine/{name}")
    void deleteEngine(@PathParam("name") String name);

    @DELETE
    @Path("{name}")
    void deleteIndex(@PathParam("name") String name);

}
