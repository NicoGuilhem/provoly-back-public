package com.provoly.ref.widget;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import com.provoly.ref.user.ProvolyUser;

@Entity
public class ProvolyUserWidgetCatalog implements Serializable {
    @Id
    @ManyToOne(cascade = CascadeType.ALL)
    private ProvolyUser user;

    @Id
    @ManyToOne
    private WidgetCatalog widgetCatalog;

    private boolean owner;

    protected ProvolyUserWidgetCatalog() {
        super();
    }

    public ProvolyUserWidgetCatalog(ProvolyUser user, WidgetCatalog request, boolean owner) {
        this.user = user;
        this.widgetCatalog = request;
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProvolyUserWidgetCatalog that = (ProvolyUserWidgetCatalog) o;
        return user.equals(that.user) && widgetCatalog.equals(that.widgetCatalog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, widgetCatalog);
    }

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }

    public WidgetCatalog getWidgetCatalog() {
        return widgetCatalog;
    }

    public void setWidgetCatalog(WidgetCatalog widgetCatalog) {
        this.widgetCatalog = widgetCatalog;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean isOwner) {
        this.owner = isOwner;
    }
}
