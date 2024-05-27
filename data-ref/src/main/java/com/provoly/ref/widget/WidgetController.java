package com.provoly.ref.widget;

import java.util.Collection;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.widget.dto.WidgetDetailsDto;
import com.provoly.ref.widget.dto.WidgetWriteDto;

@Path("/widget/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WidgetController {

    private WidgetService widgetService;
    private WidgetMapper widgetMapper;
    private UserService userService;

    WidgetController(WidgetService widgetService, UserService userService, WidgetMapper widgetMapper) {
        this.widgetService = widgetService;
        this.userService = userService;
        this.widgetMapper = widgetMapper;
    }

    @POST
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_WRITE })
    public void addWidget(WidgetWriteDto widgetDto) {
        widgetService.addWidget(widgetDto);
    }

    @GET
    @Path("/id/{id}")
    @RolesAllowed({ Role.STR_WIDGET_CATALOG_READ })
    @Transactional
    public WidgetDetailsDto getWidget(UUID id) {
        return widgetMapper.toDetailsDto(widgetService.getMineById(id));
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
        ProvolyUser provolyUser = userService.getCurrentUser();
        return widgetMapper.toCollectionWidgetDetailsDto(widgetService.getAllowedWidgets(provolyUser));
    }

}