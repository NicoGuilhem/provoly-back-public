package com.provoly.security;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AnonymousConfiguration {

    private List<String> roles;
    private List<String> groups;
    private String anonymousSub;

    private boolean anonymousEnabled;

    public AnonymousConfiguration(@ConfigProperty(name = "provoly.anonymous.roles") Optional<List<String>> maybeRoles,
            @ConfigProperty(name = "provoly.anonymous.uuid") Optional<String> maybeAnonymousSub) {
        anonymousEnabled = maybeAnonymousSub.isPresent();
        maybeRoles.ifPresentOrElse(value -> this.roles = value, () -> this.roles = List.of());
        maybeAnonymousSub.ifPresent(value -> this.anonymousSub = value);
        groups = List.of("ALL");
    }

    public List<String> getRoles() {
        return roles;
    }

    public String anonymousSub() {
        return anonymousSub;
    }

    public boolean isAnonymousEnabled() {
        return anonymousEnabled;
    }

    public List<String> getGroups() {
        return groups;
    }
}
