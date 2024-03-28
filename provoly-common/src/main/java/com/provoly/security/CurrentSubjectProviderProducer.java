package com.provoly.security;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Produces;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Dependent
public class CurrentSubjectProviderProducer {

    @Produces
    @RequestScoped
    @IfBuildProperty(name = "quarkus.oidc.enabled", stringValue = "true")
    public CurrentSubjectProvider getCurrentClaimProvider(@Claim(standard = Claims.sub) String claim,
            SecurityIdentity securityIdentity) {
        return new CurrentSubjectProvider(claim, securityIdentity);
    }
}
