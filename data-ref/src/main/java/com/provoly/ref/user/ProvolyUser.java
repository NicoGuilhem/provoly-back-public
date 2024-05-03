package com.provoly.ref.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.user.Role;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.groups.Group;

@Entity
public class ProvolyUser extends EntityId {

    private String subject; // in security identity
    private String name;
    private String lastName;
    private String email;
    @Transient
    private List<Group> groups = new ArrayList<>();
    @Transient
    private Collection<String> roles;

    @Default
    public ProvolyUser(UUID id, String subject, String name, String lastName, String email, Collection<String> roles) {
        super(id);
        this.subject = subject;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
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

    public Collection<String> getRoles() {
        return roles;
    }

    public void setRoles(Collection<String> roles) {
        this.roles = roles;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(Role.STR_ADMINISTRATE);
    }
}
