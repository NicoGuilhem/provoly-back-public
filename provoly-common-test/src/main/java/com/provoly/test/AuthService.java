package com.provoly.test;

import static io.restassured.RestAssured.oauth2;

import java.util.*;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;

import com.provoly.clients.ProvolyUserService;
import com.provoly.common.VariableType;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileDto;
import com.provoly.common.user.UserDto;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.restassured.RestAssured;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    // User and roles used in test
    // User should exist in keycloak
    public enum User {
        SUPER_ADMIN("iamsuperadmin"),

        USER("iamuser"),

        POLICE("iampolice", Map.of(UserProfile.STATUT, "policier")),
        LAWYER("iamlawyer", Map.of(UserProfile.STATUT, "lawyer"));

        final String login;
        final Map<UserProfile, String> userProfileStringMap;

        User(String login) {
            this(login, Collections.emptyMap());
        }

        User(String login, Map<UserProfile, String> userProfileMap) {
            this.login = login;
            this.userProfileStringMap = userProfileMap;
        }

        String getLogin() {
            return login;
        }

        String getPassword() {
            return "password";
        }

        public Map<UserProfile, String> getMetadata() {
            return userProfileStringMap;
        }

    }

    public enum UserProfile {
        STATUT("7f7bac11-ac2c-4f45-872a-7de32835ad13");

        private final UUID uuid;

        UserProfile(String uuid) {
            this.uuid = UUID.fromString(uuid);
        }

        String getName() {
            return this.name().toLowerCase();
        }

        public static UserProfile forDto(UserProfileDto userProfileDto) {
            for (UserProfile value : values()) {
                if (value.uuid.equals(userProfileDto.id)) {
                    return value;
                }
            }
            return null;
        }
    }

    Logger log;

    ProvolyUserService provolyUserService;

    KeycloakClient keycloak;

    private Map<User, UserDto> users;
    private Map<UserProfile, UserProfileDto> userProfileMap;
    private User currentUser;
    private String accessToken;

    public AuthService(Logger log,
            @ConfigProperty(name = "provoly.ref.mock.enabled", defaultValue = "false") boolean isMockEnabled,
            @RestClient Instance<ProvolyUserService> provolyUserService, @RestClient KeycloakClient keycloak) {
        this.log = log;
        //Hacky, Hacky to delay bean instanciation only when needed.
        if (!isMockEnabled) {
            this.provolyUserService = provolyUserService.get();
        }
        this.keycloak = keycloak;
        this.users = new HashMap<>();
    }

    /**
     * Provide the current user authenticate for DataRefClient operation
     * Replace the io.quarkus.oidc.runtime.OidcTokenCredentialProducer.currentAccessToken used in normal operation
     *
     * @return
     */
    @Produces
    @Alternative
    @Priority(10)
    AccessTokenCredential currentAccessToken() {
        return new AccessTokenCredential(accessToken);
    }

    /**
     * Inspire from :
     * https://github.com/quarkusio/quarkus/blob/29aa7daccc2032dd3ddb894e9a1b87ffe476e698/test-framework/security/src/main/java/io/quarkus/test/security/TestIdentityAssociation.java
     * Used to inject an identity when test classes annotated with QuarkusTest inject bean controller (ex: ItemsController)
     */
    @Produces
    @Alternative
    @Priority(Interceptor.Priority.LIBRARY_AFTER)
    @ApplicationScoped
    SecurityIdentityAssociation securityAssociation() {
        return new SecurityIdentityAssociation() {
            @Override
            public SecurityIdentity getIdentity() {
                return QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(currentUser.getLogin()))
                        .addCredential(currentAccessToken())
                        .addAttribute(Claims.sub.name(), users.get(currentUser).getName())
                        .addRoles(users.get(currentUser).getRoles())
                        .build();
            }
        };
    }

    /**
     * Authenticate on keycloak to retrieve the accessToken that will be used later
     */
    public void authenticate() {
        authenticate(User.SUPER_ADMIN);
    }

    public void authenticate(User user) {
        if (currentUser != user) {
            log.infof("Authenticating user %s", user);
            accessToken = keycloak.getToken(user.getLogin(), user.getPassword(), "password").access_token;
            RestAssured.authentication = oauth2(accessToken);
            this.currentUser = user;
            users.put(user, provolyUserService.getMe());
        }
    }

    public void init() {
        authenticate(User.SUPER_ADMIN);
        if (userProfileMap == null) {
            log.info("Refreshing users authorisation");
            userProfileMap = readExistingMetadataUserDef();
            createMetadataUserDef();
            authenticateAllUsers();
            setMetadataValues();
        }
        log.info("Init done");
    }

    private void setMetadataValues() {
        authenticate(User.SUPER_ADMIN);
        for (User user : User.values()) {
            var userDto = users.get(user);
            for (var metadataUserStringEntry : user.getMetadata().entrySet()) {
                MetadataValueWriteDto metadataValue = new MetadataValueWriteDto();
                metadataValue.setValue(metadataUserStringEntry.getValue());
                UserProfile userProfileName = metadataUserStringEntry.getKey();
                provolyUserService.setUserProfile(userDto.getId(), userProfileMap.get(userProfileName).id, metadataValue);
            }
        }
    }

    private void authenticateAllUsers() {
        for (User user : User.values()) {
            authenticate(user); // At least one call to register the user in the ref/database
        }
    }

    private void createMetadataUserDef() {
        for (UserProfile metadata : UserProfile.values()) {
            if (!userProfileMap.containsKey(metadata)) {
                var userProfileDto = new UserProfileDto();
                userProfileDto.id = metadata.uuid;
                userProfileDto.name = metadata.getName();
                userProfileDto.type = VariableType.STRING;
                provolyUserService.addUserProfile(userProfileDto);
                userProfileMap.put(metadata, userProfileDto);
            }
        }
    }

    private Map<UserProfile, UserProfileDto> readExistingMetadataUserDef() {
        Map<UserProfile, UserProfileDto> userProfileMap = new HashMap<>();
        for (var userProfileDto : provolyUserService.getAllUserProfiles()) {
            var userP = UserProfile.forDto(userProfileDto);
            if (userP != null) {
                userProfileMap.put(userP, userProfileDto);
            }
        }
        return userProfileMap;
    }

}
