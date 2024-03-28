package com.provoly.security;

import java.util.Collections;
import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.jwt.Claims;

public class CurrentSubjectProvider {

    // see https://quarkus.io/guides/security-jwt#using-the-jsonwebtoken-and-claim-injection
    private String currentSubject;

    private SecurityIdentity securityIdentity;

    public CurrentSubjectProvider(String currentSubject, SecurityIdentity securityIdentity) {
        this.currentSubject = currentSubject;
        this.securityIdentity = securityIdentity;
    }

    public String getSub() {
        // if sub can be injected, then it's an authenticated user, else...
        return currentSubject != null ? currentSubject : securityIdentity.getAttribute(Claims.sub.name());
    }

    public String getName() {
        return securityIdentity.getPrincipal().getName();
    }

    public String getGivenName() {
        return securityIdentity.getAttribute(Claims.given_name.name());
    }

    public Set<String> getRoles() {
        return securityIdentity.getRoles();
    }

    public Set<String> getGroups() {
        return securityIdentity.getAttribute(Claims.groups.name()) == null ? Collections.emptySet()
                : Set.copyOf(securityIdentity.getAttribute(Claims.groups.name()));
    }

    public String getEmail() {
        return securityIdentity.getAttribute(Claims.email.name());
    }

    public String getFamilyName() {
        return securityIdentity.getAttribute(Claims.family_name.name());
    }

    public String getNameBySI() {
        return securityIdentity.getAttribute(Claims.given_name.name());
    }
}
