package com.provoly.ref.widget;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;
import com.provoly.ref.widget.dto.WidgetDetailsDto;
import com.provoly.ref.widget.dto.WidgetDto;

@Path("/widget/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WidgetController {

    @Inject
    WidgetService widgetService;

    @Inject
    WidgetMapper widgetMapper;

    @POST
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_WRITE })
    public void addWidget(WidgetDto widgetDto) {
        widgetService.addWidget(widgetDto);
    }

    @GET
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_READ })
    @Transactional
    public WidgetDetailsDto getWidget(UUID id) {
        ProvolyUserWidgetCatalog provolyUserWidgetCatalog = widgetService.getMineById(id);
        return widgetMapper.toDto(provolyUserWidgetCatalog.getWidgetCatalog(), provolyUserWidgetCatalog);
    }

    @DELETE
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_WRITE })
    public void deleteWidget(UUID id) {
        widgetService.delete(id);
    }

    @GET
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_READ })
    @Transactional
    public Collection<WidgetDetailsDto> getAll() {
        return widgetMapper.toCollectionWidgetDetailsDto(widgetService.getWidgetForCurrentUser());
    }

}