package com.provoly.clients;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.WithCaching;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
import io.quarkus.security.credential.TokenCredential;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

public class ProvolyAuthentRequestFilter implements ResteasyReactiveClientRequestFilter {

    private Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter;

    private Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter;
    private Instance<TokenCredential> accessToken;

    public ProvolyAuthentRequestFilter(Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter,
            @WithCaching Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter,
            Instance<TokenCredential> accessToken) {
        this.accessTokenRequestFilter = accessTokenRequestFilter;
        this.oidcClientRequestFilter = oidcClientRequestFilter;
        this.accessToken = accessToken;
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        ManagedContext managedContext = Arc.container()
                .requestContext();
        if (managedContext != null &&
                managedContext.isActive() &&
                accessToken.isResolvable() &&
                accessToken.get().getToken() != null) {
            accessTokenRequestFilter.get().filter(requestContext);
            return;
        }
        if (managedContext == null ||
                !managedContext.isActive()) {
            oidcClientRequestFilter.get().filter(requestContext);
        }
    }
}
