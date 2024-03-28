package com.provoly.ref.user;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ProvolyUserNamedQuery implements Serializable {

    @Id
    @ManyToOne(cascade = CascadeType.ALL)
    private ProvolyUser user;

    @Id
    @ManyToOne
    private NamedQuery namedQuery;

    private boolean favorite;
    private String color;

    private Instant lastExecutionDate;

    private boolean owner;

    protected ProvolyUserNamedQuery() {
        // Only for JPA
    }

    public ProvolyUserNamedQuery(ProvolyUser user, NamedQuery request, boolean owner) {
        //this.id = new Id(user, request);
        this.user = user;
        this.namedQuery = request;
        this.favorite = false;
        this.lastExecutionDate = null;
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProvolyUserNamedQuery that = (ProvolyUserNamedQuery) o;
        return user.equals(that.user) && namedQuery.equals(that.namedQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, namedQuery);
    }

    public ProvolyUser getUser() {
        return user;
    }

    public void setUser(ProvolyUser user) {
        this.user = user;
    }

    public NamedQuery getNamedQuery() {
        return namedQuery;
    }

    public void setNamedQuery(NamedQuery namedQuery) {
        this.namedQuery = namedQuery;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Instant getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(Instant lastExecution) {
        this.lastExecutionDate = lastExecution;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean isOwner) {
        this.owner = isOwner;
    }
}
