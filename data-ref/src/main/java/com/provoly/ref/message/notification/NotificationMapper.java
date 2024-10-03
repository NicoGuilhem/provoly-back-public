package com.provoly.ref.message.notification;

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
        notification.getParameterValues().forEach(param -> parameterMap.put(param.getKey(), param.getValue()));
        var notificationTextDto = new NotificationTextDto(notification.getTitleCode(), notification.getMessageCode(),
                parameterMap);
        return new NotificationPayload(notificationTextDto, notification.getLink(),
                notification.getCreationDate());
    }

    public Notification toModel(NotificationRequestDto dto) {
        var notification = new Notification(dto.id());
        notification.setLink(dto.link());
        notification.setMessageCode(dto.notificationTextDto().code());
        notification.setTitleCode(dto.notificationTextDto().title());

        var parameters = dto.notificationTextDto().param()
                .entrySet()
                .stream()
                .map(param -> new NotificationParameter(UUID.randomUUID(), notification, param.getKey(), param.getValue()))
                .toList();

        notification.getParameterValues().addAll(parameters);

        return notification;
    }

}