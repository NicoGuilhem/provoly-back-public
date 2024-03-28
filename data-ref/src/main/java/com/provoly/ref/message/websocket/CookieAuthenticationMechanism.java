package com.provoly.ref.message.websocket;

import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.BearerAuthenticationMechanism;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(Priorities.USER)
@ApplicationScoped
public class CookieAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";
    @Inject
    OidcAuthenticationMechanism oidc;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        if (context.request().path().endsWith("/messages")) {
            var cookie = context.request().getCookie("token");
            if (cookie == null) {
                return oidc.authenticate(context, identityProviderManager);
            } else {
                context.request().headers().add(AUTHORIZATION_HEADER, BEARER + cookie.getValue());
                return new BearerAuthenticationMechanism().authenticate(context, identityProviderManager,
                        new OidcTenantConfig());
            }
        }
        return oidc.authenticate(context, identityProviderManager);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return oidc.getChallenge(context);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        Set<Class<? extends AuthenticationRequest>> credentialTypes = new HashSet<>();
        credentialTypes.addAll(oidc.getCredentialTypes());
        return credentialTypes;
    }

}
