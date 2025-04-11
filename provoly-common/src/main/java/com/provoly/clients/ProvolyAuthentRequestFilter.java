package com.provoly.clients;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.WithCaching;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
import io.quarkus.security.credential.TokenCredential;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

public class ProvolyAuthentRequestFilter implements ResteasyReactiveClientRequestFilter {

    private final Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter;

    private final Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter;
    private final Instance<TokenCredential> accessToken;
    private final Logger log;


    public ProvolyAuthentRequestFilter(Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter,
                                       @WithCaching Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter,
                                       Instance<TokenCredential> accessToken, Logger log) {
        log.info("Building a new ProvolyAuthentRequestFilter");
        this.accessTokenRequestFilter = accessTokenRequestFilter;
        this.oidcClientRequestFilter = oidcClientRequestFilter;
        this.accessToken = accessToken;
        this.log = log;
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        ManagedContext managedContext = Arc.container().requestContext();
        if (managedContext != null &&
                managedContext.isActive() &&
                accessToken.isResolvable() &&
                accessToken.get().getToken() != null) {
            log.debugf("Request filter using a access token : %s", requestContext.getUri());
            accessTokenRequestFilter.get().filter(requestContext);
            return;
        }
        if (managedContext == null ||
                !managedContext.isActive()) {
            log.debugf("Request filter using a oidc client request : %s", requestContext.getUri());
            oidcClientRequestFilter.get().filter(requestContext);
        }
    }
}
