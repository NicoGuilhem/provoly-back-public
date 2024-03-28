package com.provoly.ref.datasource;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.user.Role;

@Path("/data-sources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DataSourceController {

    private DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GET
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{dataSourceId}")
    public DataSourceDetailsDto getDatasourceDetail(UUID dataSourceId) {
        return dataSourceService.getDataSourceDetails(dataSourceId);
    }

}
