package com.provoly.exec;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.exec.JobModelDto;
import com.provoly.common.exec.ParameterDto;
import com.provoly.common.exec.ParameterFileDto;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JobModelControllerTest {

    private static final String IMAGE_NAME = "imageName";
    private static final String JOB_MODEL_URL = "/job/models";
    private static final String IMAGE_NAME2 = "imageName2";

    @Inject
    JobModelService jobModelService;

    public JobModelDto createJobModel(UUID id, String image) {
        Set<ParameterDto> parameters = Set.of(new ParameterFileDto("name", "file.json"));
        return new JobModelDto(id, image, parameters);
    }

    @AfterEach
    public void cleanJobModel() {
        for (var model : jobModelService.getAll()) {
            jobModelService.deleteJobModelById(model.getId());
        }
    }

    @Test
    public void should_addJobModel_return200() {
        var id = UUID.randomUUID();
        var jobModelDto = createJobModel(id, IMAGE_NAME);

        given()
                .body(jobModelDto)
                .contentType(ContentType.JSON)
                .when()
                .put(JOB_MODEL_URL)
                .then()
                .statusCode(204);
    }

    @Test
    public void should_getJobModel_return200() {
        var id = UUID.randomUUID();
        var jobModelDto = createJobModel(id, IMAGE_NAME);

        jobModelService.save(jobModelDto);

        given()
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .when()
                .get("%s/id/{id}".formatted(JOB_MODEL_URL))
                .then()
                .statusCode(200)
                .body("id", is(id.toString()));
    }

    @Test
    public void should_getJobModel_return404() {
        var id = UUID.randomUUID();
        given()
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .when()
                .get("%s/id/{id}".formatted(JOB_MODEL_URL))
                .then()
                .statusCode(404)
                .body("message", is("JobModel : %s inexistant.".formatted(id)))
                .body("code", is("NOT_FOUND"));
    }

    @Test
    public void should_updateJobModel_return200() {
        var id = UUID.randomUUID();
        var jobModelDto = createJobModel(id, IMAGE_NAME);
        var jobModelDtoUpdated = createJobModel(id, IMAGE_NAME2);
        jobModelService.save(jobModelDto);

        given()
                .body(jobModelDtoUpdated)
                .contentType(ContentType.JSON)
                .when()
                .put(JOB_MODEL_URL)
                .then()
                .statusCode(204);
    }

    @Test
    public void should_getAllJobModel_return200() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        var jobModelDto = createJobModel(firstId, IMAGE_NAME);
        jobModelService.save(jobModelDto);
        var jobModelDto2 = createJobModel(secondId, IMAGE_NAME2);
        jobModelService.save(jobModelDto2);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOB_MODEL_URL)
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    public void should_getAllWithoutJobModel_return200() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOB_MODEL_URL)
                .then()
                .statusCode(200)
                .body("size()", is(0))
                .body(is("[]"));
    }

    @Test
    public void should_deleteExistingJobModel_return204() {
        UUID id = UUID.randomUUID();
        JobModelDto myJobModel = createJobModel(id, "Image1");
        jobModelService.save(myJobModel);

        given()
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .when()
                .delete("%s/id/{id}".formatted(JOB_MODEL_URL))
                .then()
                .statusCode(204);
    }

    @Test
    public void should_deleteMissingJobModel_return404() {
        UUID id = UUID.randomUUID();

        given()
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .when()
                .delete("%s/id/{id}".formatted(JOB_MODEL_URL))
                .then()
                .statusCode(404);
    }
}
