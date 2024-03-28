package com.provoly.version;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/about")
public class VersionController {

    @ConfigProperty(name = "provoly.application.version")
    String applicationVersion;

    @ConfigProperty(name = "provoly.application.chart.version")
    String chartVersion;

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    public Version getVersion() {
        return new Version(applicationVersion, chartVersion);
    }

    public record Version(String applicationVersion, String chartVersion) {
    }
}
