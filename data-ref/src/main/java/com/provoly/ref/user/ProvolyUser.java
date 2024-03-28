package com.provoly.ref.user;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.groups.Group;

@Entity
public class ProvolyUser extends EntityId {

    private String subject; // in security identity
    private String name;
    private String lastName;
    private String email;
    @Transient
    private List<Group> groups;

    @Default
    public ProvolyUser(UUID id, String subject, String name, String lastName, String email) {
        super(id);
        this.subject = subject;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
    }

    public ProvolyUser() {
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "ProvolyUser{id='" + getId() + "', subject='" + subject + "'}";
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
