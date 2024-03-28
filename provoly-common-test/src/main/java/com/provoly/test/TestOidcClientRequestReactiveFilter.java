package com.provoly.test;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;

import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;

/**
 * Used for tests. It retrieves authService accessToken if it exists, otherwise, use OidcClientRequestReactiveFilter mechanism
 */
@Priority(Priorities.AUTHENTICATION - 1)
@ApplicationScoped
@Alternative
public class TestOidcClientRequestReactiveFilter extends OidcClientRequestReactiveFilter {

    @Inject
    AuthService authService;

    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.suspend();
        if (authService.currentAccessToken() != null) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                    "Bearer " + authService.currentAccessToken().getToken());
            requestContext.resume();
            return;
        }

        super.filter(requestContext);
    }
}
