package com.provoly.ref.dashboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.groups.GroupErrors;
import com.provoly.ref.metadata.MetadataService;

import org.jboss.resteasy.reactive.RestResponse;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/users/me/dashboards")
public class DashboardController {

    private DashboardMapper dashboardMapper;
    private DashboardService dashboardService;
    private MetadataService metadataService;

    public DashboardController(DashboardMapper dashboardMapper, DashboardService dashboardService,
            MetadataService metadataService) {
        this.dashboardMapper = dashboardMapper;
        this.dashboardService = dashboardService;
        this.metadataService = metadataService;
    }

    @POST
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE })
    public RestResponse<GroupErrors> saveDashboard(DashboardWriteDto dashboardDto) {
        var groupErrors = dashboardService.saveOrUpdate(dashboardDto);
        return RestResponse.ResponseBuilder.ok(groupErrors).build();
    }

    @GET
    @RolesAllowed({ Role.STR_DASHBOARD_READ })
    public List<DashboardReadDto> getAllForCurrentUser() {
        return dashboardMapper.toReadDto(dashboardService.getCurrentUserAllowedDashboards());
    }

    @GET
    @RolesAllowed({ Role.STR_DASHBOARD_READ })
    @Path("/id/{id}")
    public DashboardReadDto getById(UUID id) {
        return dashboardMapper.toReadDto(dashboardService.getCurrentUserAllowedDashboardById(id));
    }

    @GET
    @RolesAllowed({ Role.STR_DASHBOARD_READ })
    @Path("/id/{id}/manifest")
    public Map<String, Object> getDashBoardManifest(UUID id) {
        return dashboardService.getCurrentUserAllowedDashboardById(id).getManifest();
    }

    @DELETE
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE })
    @Path("/id/{id}")
    public void delete(UUID id) {
        dashboardService.delete(id);
    }

    @PUT
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE })
    @Path("/id/{dashboardId}/metadata/id/{metadataDefId}")
    public void setMetadata(@PathParam("dashboardId") UUID dashboardId, @PathParam("metadataDefId") UUID metadataDefId,
            MetadataValueWriteDto metadata) {
        metadataService.addMetadataToEntity(dashboardId, metadataDefId, metadata, EntityType.DASHBOARD);
    }

    @DELETE
    @Path("/id/{dashboardId}/metadata/id/{metadataDefId}")
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE })
    public void deleteMetadata(@PathParam("dashboardId") UUID dashboardId, @PathParam("metadataDefId") UUID metadataDefId) {
        metadataService.deleteMetadataValueByEntityId(dashboardId, metadataDefId, EntityType.DASHBOARD);
    }

}