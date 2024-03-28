package com.provoly.version;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class VersionControllerTest {

    @Test
    public void getVersionTest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/about/version")
                .then()
                .statusCode(200)
                .body("applicationVersion", equalTo("test"))
                .body("chartVersion", equalTo("test"));
    }
}
