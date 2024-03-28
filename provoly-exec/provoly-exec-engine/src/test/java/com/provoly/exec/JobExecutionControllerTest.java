package com.provoly.exec;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.exec.*;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyTestContainers;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
public class JobExecutionControllerTest {
    @Inject
    JobExecutionService jobExecutionService;

    @Inject
    JobInstanceService jobInstanceService;

    @Inject
    JobModelService jobModelService;

    @Inject
    AuthService authService;

    private static final String JOB_MODEL_URL = "/job/executions";

    public void authenticate() {
        authService.authenticate();
    }

    @AfterEach
    public void cleanJobExecutions() {
        for (var exec : jobExecutionService.getAll()) {
            jobExecutionService.delete(exec.getId());
        }

        for (var instance : jobInstanceService.getAllInstances()) {
            jobInstanceService.deleteById(instance.getId());
        }

        for (var model : jobModelService.getAll()) {
            jobModelService.deleteJobModelById(model.getId());
        }
    }

    public JobModelDto createJobModel(UUID id) {
        Set<ParameterDto> parameters = Set.of(new ParameterFileDto("name", "file.json"));
        return new JobModelDto(id, "image", parameters);
    }

    public JobInstanceDto createInstance(UUID id, UUID idModel) {
        return new JobInstanceDto(id, idModel, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    @Test
    public void should_getEmptyListJobExecution_return200() {
        authenticate();
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOB_MODEL_URL)
                .then()
                .statusCode(200)
                .body(is("[]"))
                .body("size()", is(0));
    }

    @Test
    public void should_getEmptyListJobExecution_wrongRole_return401() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(JOB_MODEL_URL)
                .then()
                .statusCode(401);
    }

    @Test
    public void should_getJobExecById_return200() {
        authenticate();
        var id = UUID.randomUUID();
        var idModel = UUID.randomUUID();
        var idInstance = UUID.randomUUID();
        var jobModelDto = createJobModel(idModel);
        var jobInstanceDto = createInstance(idInstance, idModel);
        jobModelService.save(jobModelDto);
        jobInstanceService.save(jobInstanceDto);
        var jobExecutionDto = new JobExecutionDto(id, idInstance);
        jobExecutionService.save(jobExecutionDto);

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
    public void should_throwErrorWhenGetNonexistent_return404() {
        authenticate();
        given()
                .pathParam("id", UUID.randomUUID())
                .contentType(ContentType.JSON)
                .when()
                .get("%s/id/{id}".formatted(JOB_MODEL_URL))
                .then()
                .statusCode(404);
    }
}
