package com.provoly.ref.datasource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.Role;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class DataSourceControllerTest {

    @InjectMock
    DataSourceService dataSourceService;

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void getDatasourceDetail_WithDataSetId_Return200() {
        UUID id = UUID.randomUUID();
        DataSourceDetailsDto dto = new DataSourceDetailsDto(id, DataSourceType.DATASET_VERSION, UUID.randomUUID());

        when(dataSourceService.getDataSourceDetails(id)).thenReturn(dto);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("data-sources/id/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("type", equalTo("DATASET_VERSION"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void getDatasourceDetail_WithNamedQueryId_Return200() {
        UUID id = UUID.randomUUID();
        DataSourceDetailsDto dto = new DataSourceDetailsDto(id, DataSourceType.SEARCH, UUID.randomUUID());

        when(dataSourceService.getDataSourceDetails(id)).thenReturn(dto);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("data-sources/id/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("type", equalTo("SEARCH"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_SEARCH })
    public void getDatasourceDetail_WithUnknownId_Return404() {
        UUID id = UUID.randomUUID();

        when(dataSourceService.getDataSourceDetails(id))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "DataSource not found: " + id));

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/data-sources/id/" + id)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }
}
