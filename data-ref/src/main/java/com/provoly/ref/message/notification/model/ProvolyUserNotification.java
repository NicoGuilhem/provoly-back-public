package com.provoly.ref.message.notification.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import com.provoly.ref.user.ProvolyUser;

@Entity
public class ProvolyUserNotification implements Serializable {

    @Id
    @ManyToOne(cascade = CascadeType.ALL)
    private ProvolyUser user;

    @Id
    @ManyToOne
    private Notification notification;

    protected ProvolyUserNotification() {
        // Only for JPA
    }

    public ProvolyUserNotification(ProvolyUser user, Notification notification) {
        this.user = user;
        this.notification = notification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProvolyUserNotification that = (ProvolyUserNotification) o;
        return user.equals(that.user) && notification.equals(that.notification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, notification);
    }

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

}
