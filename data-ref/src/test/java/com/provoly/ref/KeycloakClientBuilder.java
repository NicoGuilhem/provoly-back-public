package com.provoly.ref;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@ApplicationScoped
public class KeycloakClientBuilder {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String url;

    @ConfigProperty(name = "provoly.ref.user")
    String username;

    @ConfigProperty(name = "provoly.ref.password")
    String password;

    public Keycloak build() {
        return KeycloakBuilder.builder()
                .serverUrl(urlWithoutRealm(url))
                .realm("provoly")
                .username(username)
                .password(password)
                .clientId("admin-cli")
                .build();
    }

    /**
     * @param url : A url in form : https://&lt;hostname&gt;/auth/realms/&lt;realm&gt;
     * @return url in the form https://sso.dev.provoly.net/auth
     */
    private String urlWithoutRealm(String url) {
        var index = url.lastIndexOf("/realms/");
        if (index == -1) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to build the keycloak url from " + url);
        }
        return url.substring(0, index);
    }
}
