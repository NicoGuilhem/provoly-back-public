package com.provoly.security;

import static com.provoly.common.user.Role.STR_ADMINISTRATE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;

import com.provoly.common.user.Role;
import com.provoly.common.user.SystemGroup;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RolesAugmentorUTest {

    static RolesAugmentor rolesAugmentor;

    static AnonymousConfiguration anonymousConfiguration;
    static AuthenticationRequestContext authenticationRequestContext;

    @BeforeAll
    static void before() {
        anonymousConfiguration = mock(AnonymousConfiguration.class);
        rolesAugmentor = new RolesAugmentor(anonymousConfiguration);
        authenticationRequestContext = mock(AuthenticationRequestContext.class);
    }

    @Test
    void administrate_role_should_add_all_roles() {
        when(anonymousConfiguration.isAnonymousEnabled()).thenReturn(false);

        var mockOidcJwt  = mock(OidcJwtCallerPrincipal.class);
        var mockSecurityIdentity = mock(SecurityIdentity.class);
        when(mockSecurityIdentity.getRoles()).thenReturn(Set.of(STR_ADMINISTRATE));
        when(mockSecurityIdentity.getPrincipal()).thenReturn(mockOidcJwt);

        UniAssertSubscriber<SecurityIdentity> identityUni = rolesAugmentor.augment(mockSecurityIdentity, authenticationRequestContext)
                .invoke( identity ->
                        Assertions.assertTrue(
                                identity.getRoles()
                                        .containsAll(Arrays.stream(Role.values()).map(r -> r.toString().toLowerCase()).collect(Collectors.toSet()))))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        identityUni.assertCompleted();
    }

    @Test
    void oidc_user_should_have_all_and_authenticated_groups() {
        when(anonymousConfiguration.isAnonymousEnabled()).thenReturn(false);

        var oidcJwtt  = new OidcJwtCallerPrincipal(new JwtClaims(), new TokenCredential("", ""));
        var mockSecurityIdentity = mock(SecurityIdentity.class);
        when(mockSecurityIdentity.getRoles()).thenReturn(Set.of(STR_ADMINISTRATE));
        when(mockSecurityIdentity.getPrincipal()).thenReturn(oidcJwtt);

        UniAssertSubscriber<SecurityIdentity> identityUni = rolesAugmentor.augment(mockSecurityIdentity, authenticationRequestContext)
                .invoke( identity -> {
                    var groups = (HashSet<String>) identity.getAttribute(Claims.groups.name());
                    Assertions.assertTrue(groups.containsAll(Set.of(SystemGroup.AUTHENTICATED.name(), SystemGroup.ALL.name())));
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        identityUni.assertCompleted();
    }

    @Test
    void anonymous_user_should_have_all_groups() {
        when(anonymousConfiguration.isAnonymousEnabled()).thenReturn(true);

        var oidcJwt  = new OidcJwtCallerPrincipal(new JwtClaims(), new TokenCredential("", ""));
        var mockSecurityIdentity = mock(SecurityIdentity.class);
        when(mockSecurityIdentity.isAnonymous()).thenReturn(true);
        when(mockSecurityIdentity.getRoles()).thenReturn(Set.of(STR_ADMINISTRATE));
        when(mockSecurityIdentity.getPrincipal()).thenReturn(oidcJwt);

        UniAssertSubscriber<SecurityIdentity> identityUni = rolesAugmentor.augment(mockSecurityIdentity, authenticationRequestContext)
                .invoke( identity -> {
                    var groups = (List<String>) identity.getAttribute(Claims.groups.name());
                    Assertions.assertTrue(groups.contains(SystemGroup.ALL.name()));
                    Assertions.assertFalse(groups.contains(SystemGroup.AUTHENTICATED.name()));
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        identityUni.assertCompleted();
    }
}
