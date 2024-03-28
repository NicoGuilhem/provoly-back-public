package com.provoly.ref.message;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.ref.message.notification.NotificationMapper;
import com.provoly.ref.message.notification.model.Notification;
import com.provoly.ref.message.websocket.MessageSocketServer;

@ApplicationScoped
public class MessageService {

    @Inject
    MessageSocketServer messageSocketServer;

    @Inject
    NotificationMapper notificationMapper;

    public void sendMessage(Message message, UUID user) {
        sendMessage(message, List.of(user));
    }

    public void sendMessage(Message message, List<UUID> users) {
        messageSocketServer.sendMessage(message, users);
    }

    public Message createMessage(Notification notification) {
        var notificationPayload = notificationMapper.toPayload(notification);
        return new Message(MessageType.NOTIFICATION, notification.getId(), notificationPayload);
    }

    public void sendMessage(Message message, String sessionId) {
        messageSocketServer.sendMessage(message, sessionId);
    }
}