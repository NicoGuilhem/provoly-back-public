package com.provoly.ref.user;

import static org.hamcrest.CoreMatchers.is;

import java.util.Map;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AnonymousUserControllerTest.AnonymousTestProfile.class)
class AnonymousUserControllerTest {

    @ConfigProperty(name = "provoly.anonymous.uuid")
    Optional<String> anonymousUUID;

    @Test
    void should_getAnonymous() {

        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("users/me")
                .then()
                .statusCode(200)
                .body("id", is(anonymousUUID.get()));
    }

    public static class AnonymousTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("provoly.anonymous.uuid", "1fb6c658-c02e-4531-996e-c37b9eefc6b3", "provoly.anonymous.roles",
                    "foo,bar");
        }
    }

}
