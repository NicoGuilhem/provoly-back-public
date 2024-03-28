package com.provoly.ref.message.notification.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.provoly.ref.entity.EntityId;

@Entity
public class NotificationParameter extends EntityId {

    @ManyToOne
    private Notification notification;

    private String key;

    private String value;

    protected NotificationParameter() {
        super();
    }

    public NotificationParameter(UUID id) {
        this.id = id;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}