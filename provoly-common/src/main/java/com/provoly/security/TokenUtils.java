package com.provoly.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.WithCaching;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.security.credential.TokenCredential;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;

@ApplicationScoped
public class TokenUtils {

    @Inject
    Instance<TokenCredential> accessToken;

    @Inject
    @WithCaching
    Instance<OidcClientRequestReactiveFilter> oidcClientRequestFilter;

    public String getToken() {
        InjectableContext activeContext = Arc.container().getActiveContext(RequestScoped.class);
        if (activeContext != null && CurrentRequestManager.get() != null) {
            return accessToken.get().getToken();
        }
        return oidcClientRequestFilter.get().getTokens().await().indefinitely().getAccessToken();
    }
}
