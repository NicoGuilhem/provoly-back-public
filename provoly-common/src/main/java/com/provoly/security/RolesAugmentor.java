package com.provoly.security;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.user.SystemGroup;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class RolesAugmentor implements SecurityIdentityAugmentor {

    private AnonymousConfiguration anonymousConf;

    public RolesAugmentor(AnonymousConfiguration anonymousConf) {
        this.anonymousConf = anonymousConf;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (!anonymousConf.isAnonymousEnabled())
            return Uni.createFrom().item(buildPrincipal(identity));
        return Uni.createFrom().item(build(identity));
    }

    private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            builder.setAnonymous(false);
            builder.addAttribute(Claims.sub.name(), anonymousConf.anonymousSub());
            anonymousConf.getRoles().forEach(builder::addRole);
            builder.setPrincipal(() -> "anonymous");
            builder.addAttribute(Claims.groups.name(), List.of(SystemGroup.ALL.name()));
            return builder::build;
        } else {
            return buildPrincipal(identity);
        }
    }

    private Supplier<SecurityIdentity> buildPrincipal(SecurityIdentity identity) {
        if (identity.getPrincipal() instanceof OidcJwtCallerPrincipal principal) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            var groups = principal.getGroups();
            groups.add(SystemGroup.AUTHENTICATED.name());
            groups.add(SystemGroup.ALL.name());
            builder.addAttribute(Claims.groups.name(), groups);
            builder.addAttribute(Claims.family_name.name(), principal.getClaim(Claims.family_name.name()));
            builder.addAttribute(Claims.given_name.name(), principal.getClaim(Claims.given_name.name()));
            builder.addAttribute(Claims.email.name(), principal.getClaim(Claims.email.name()));
            return builder::build;
        } else {
            return QuarkusSecurityIdentity.builder(identity)::build;
        }
    }

}
