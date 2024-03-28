package com.provoly.ref.abac;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.abac.PredicateDto;
import com.provoly.ref.abac.predicate.Predicate;
import com.provoly.ref.abac.predicate.PredicateService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PredicateControllerTest {

    private final UUID id = UUID.randomUUID();
    private final Predicate pred = new Predicate(id);
    @Inject
    PredicateService predicateService;

    @BeforeEach
    public void addPredicate() {
        pred.setName("false");
        pred.setValue("false");
        predicateService.save(pred);
    }

    @Test
    public void addPredicate_notAuthorized() {
        PredicateDto dto = new PredicateDto();
        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/predicates")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "data_access_write" })
    public void AddPredicate_returnOk() {
        PredicateDto dto = new PredicateDto();
        dto.id = UUID.randomUUID();
        dto.name = "true";
        dto.value = "true";
        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/predicates")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "data_access_read" })
    public void getPredicate_returnOk() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/predicates/id/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("name", equalTo("false"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "data_access_read" })
    public void shouldGetAllPredicate() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/predicates")
                .then()
                .statusCode(200);
    }
}
