package com.provoly.ref.message.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.ref.message.notification.dto.NotificationRequestDto;
import com.provoly.ref.message.notification.dto.NotificationTextDto;
import com.provoly.ref.message.notification.model.Notification;
import com.provoly.ref.message.notification.model.NotificationParameter;

@ApplicationScoped
public class NotificationMapper {

    public NotificationPayload toPayload(Notification notification) {
        var parameterMap = new HashMap<String, String>();
        notification.getParameterValues().stream().forEach(param -> parameterMap.put(param.getKey(), param.getValue()));
        var notificationTextDto = new NotificationTextDto(notification.getMessageCode(), parameterMap);
        return new NotificationPayload(notificationTextDto, notification.getLink(),
                notification.getCreationDate());
    }

    public Notification toModel(NotificationRequestDto dto) {
        var notif = new Notification(dto.id());
        notif.setLink(dto.link());
        notif.setMessageCode(dto.notificationTextDto().code());

        var parameters = new ArrayList<NotificationParameter>();
        dto.notificationTextDto().param().entrySet().forEach(entry -> {
            var notifParameter = new NotificationParameter(UUID.randomUUID());
            notifParameter.setKey(entry.getKey());
            notifParameter.setValue(entry.getValue());
            notifParameter.setNotification(notif);

            parameters.add(notifParameter);
        });
        notif.getParameterValues().addAll(parameters);

        return notif;
    }

}