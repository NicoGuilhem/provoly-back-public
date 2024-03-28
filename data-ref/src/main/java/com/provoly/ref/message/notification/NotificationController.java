package com.provoly.ref.message.notification;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;
import com.provoly.ref.message.notification.dto.NotificationRequestDto;

@Path("/notification")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationController {

    @Inject
    NotificationService notificationService;

    @POST
    @RolesAllowed({ Role.STR_CLASS_WRITE_NOTIFICATION })
    public void addNotification(NotificationRequestDto notificationDto) {
        notificationService.addNotification(notificationDto);
    }

    @DELETE
    @Path("/me/id/{id}")
    public void acknowledgeNotification(UUID id) {
        notificationService.acknowledgeNotification(id);
    }

    @DELETE
    @Path("/me")
    public void acknowledgeAllNotification() {
        notificationService.acknowledgeAllNotificationForCurrentUser();
    }
}