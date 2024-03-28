package com.provoly.ref.message;

import java.util.UUID;

import com.provoly.ref.message.notification.NotificationPayload;

public class Message {

    private final MessageType type;
    private final UUID id;
    private final NotificationPayload payload;

    public Message(MessageType type, UUID id, NotificationPayload payload) {
        this.type = type;
        this.id = id;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", id=" + id +
                ", payload=" + payload +
                '}';
    }

    public MessageType getType() {
        return type;
    }

    public UUID getId() {
        return id;
    }

    public NotificationPayload getPayload() {
        return payload;
    }

}
