package com.provoly.clients;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
import io.quarkus.security.credential.TokenCredential;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

/**
 * This filter is used to propagate the access token to the REST client.
 * It will use the AccessTokenRequestReactiveFilter if the access token is available,
 * otherwise it will use the OidcClientRequestReactiveFilter.
 * Caveat: Seems we have one token by client (one for each Interface).
 * Not a problem if we have multiple OidcClientRequestReactiveFilter has it use a static field for storing tokens
 */
public class ProvolyAuthentRequestFilter implements ResteasyReactiveClientRequestFilter {

    private final Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter;

    private final OidcClientRequestReactiveFilter oidcClientFilter;
    private final Instance<TokenCredential> accessToken;
    private final Logger log;

    public ProvolyAuthentRequestFilter(Instance<AccessTokenRequestReactiveFilter> accessTokenRequestFilter,
            Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter,
            Instance<TokenCredential> accessToken, Logger log) {
        log.info("Building a new ProvolyAuthentRequestFilter");
        this.accessTokenRequestFilter = accessTokenRequestFilter;
        this.oidcClientFilter = oidcClientRequestFilter.get();
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
            log.debugf("Auth request filter using a context access token : %s", requestContext.getUri());
            accessTokenRequestFilter.get().filter(requestContext);
            return;
        }
        if (managedContext == null || !managedContext.isActive()) {
            log.debugf("Auth request filter using oidc client request : %s", requestContext.getUri());
            oidcClientFilter.filter(requestContext);
            return;
        }
        log.debugf("Auth request filter skipped : %s", requestContext.getUri());
    }
}
