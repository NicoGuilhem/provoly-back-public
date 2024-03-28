package com.provoly.ref.oClass;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.dataset.DatasetMapper;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class OClassControllerTest {
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper mapper;
    @Inject
    DatasetService datasetService;
    @Inject
    UserService userService;
    @Inject
    TestService testService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    DatasetMapper datasetMapper;

    private OClass oClass;

    @AfterEach
    @Transactional
    public void clean() {
        testService.authenticate("iampolice", currentSubjectProvider);
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_returnOk() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), "attr", "technical_attr", field.id);
        AttributeDefDto attributeDefDto2 = testService.createAttributeDto(UUID.randomUUID(), "attr2", "technical_attr2",
                field.id);
        OClassWriteDto oClassDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDto,
                attributeDefDto2);

        given()
                .body(oClassDto)
                .contentType(ContentType.JSON)
                .when()
                .post("model/class")
                .then()
                .statusCode(204)
                .body(is(""));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_read" })
    public void getClass_returnOk_withCorrectAttrNames() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        var className = "oclass";
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), "attr", "technical_attr", field.id);
        AttributeDefDto attributeDefDto2 = testService.createAttributeDto(UUID.randomUUID(), "attr2", "technical_attr2",
                field.id);
        OClassWriteDto oClassDto = testService.createClassWriteDto(id, className, attributeDefDto, attributeDefDto2);
        modelService.saveClass(oClassDto);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/model/class/id/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("name", equalTo(className + "-" + id))
                .body("attributes.size()", is(2))
                .body("attributes.find { it.name == 'attr' }.technicalName", is("technical_attr"))
                .body("attributes.find { it.name == 'attr2' }.technicalName", is("technical_attr2"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_read" })
    public void getClass_withMissingName_returnOk_withCorrectAttrNames() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        var className = "oclass";
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), null,
                "a text with more than fifty characters to show the substring rule", field.id);
        AttributeDefDto attributeDefDto2 = testService.createAttributeDto(UUID.randomUUID(), "attr2", "technical_attr2",
                field.id);
        OClassWriteDto oClassDto = testService.createClassWriteDto(id, className, attributeDefDto, attributeDefDto2);
        modelService.saveClass(oClassDto);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/model/class/id/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("name", equalTo(className + "-" + id))
                .body("attributes.size()", is(2))
                .body("attributes.find { it.name == 'a text with more than fifty characters to show the' }.technicalName",
                        is("a text with more than fifty characters to show the substring rule"))
                .body("attributes.find { it.name == 'attr2' }.technicalName", is("technical_attr2"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_noField_return_400() {
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), "attr", "attr", UUID.randomUUID());
        OClassWriteDto oClassDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDto);

        given()
                .body(oClassDto)
                .contentType(ContentType.JSON)
                .when()
                .post("model/class")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_attrSameId_return_400() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        AttributeDefDto attributeDefDetailsDto = testService.createAttributeDto(id, "attr", "attr", field.id);
        AttributeDefDto attributeDefDetailsDto2 = testService.createAttributeDto(id, "attr2", "attr2", field.id);
        OClassWriteDto oclassWriteDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDetailsDto,
                attributeDefDetailsDto2);

        given()
                .body(oclassWriteDto)
                .contentType(ContentType.JSON)
                .when()
                .post("model/class")
                .then()
                .statusCode(409)
                .body("message", is("Duplicate id in attributes"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_withoutTechnicalName_return_400() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        AttributeDefDto attributeDefDetailsDto = testService.createAttributeDto(id, "attr", null, field.id);
        AttributeDefDto attributeDefDetailsDto2 = testService.createAttributeDto(id, "attr2", "attr2", field.id);
        OClassWriteDto oclassWriteDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDetailsDto,
                attributeDefDetailsDto2);

        given()
                .body(oclassWriteDto)
                .contentType(ContentType.JSON)
                .when()
                .post("model/class")
                .then()
                .statusCode(400)
                .body("message", is("Missing technical name in attribute %s".formatted(attributeDefDetailsDto.id)));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_attrSameIdInOtherOclass_return_400() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        var attributeDefDto1 = testService.createAttributeDto(id, "attribute1", "attribute1", field.id);
        var attributeDefDto2 = testService.createAttributeDto(id, "attribute2", "attribute2", field.id);
        var oClassDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDto1);
        oClass = mapper.toModel(oClassDto);
        modelService.saveClass(oClassDto);

        OClassWriteDto oClassDto2 = testService.createClassWriteDto(UUID.randomUUID(), "oclass2", attributeDefDto2);

        given()
                .body(oClassDto2)
                .contentType(ContentType.JSON)
                .when()
                .post("model/class")
                .then()
                .statusCode(409)
                .body("message", containsString(id.toString()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void deleteClass_datasetExist_return_403() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        ProvolyUser provolyUser = userService.getCurrentUser();
        var field = testService.createAndSaveField();
        AttributeDefDto attributeDefDto = testService.createAttributeDto(UUID.randomUUID(), "attr", "attr", field.id);

        OClassWriteDto oClassDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", attributeDefDto);
        oClass = mapper.toModel(oClassDto);
        modelService.saveClass(oClassDto);

        var datasetDto = new DatasetDto(oClassDto.getId(), "name", oClass.getId(), DatasetType.MODIFIABLE);
        var dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetService.saveEntity(dataset);

        given()
                .pathParam("id", oClassDto.getId())
                .body(oClassDto)
                .contentType(ContentType.JSON)
                .when()
                .delete("model/class/id/{id}")
                .then()
                .statusCode(403)
                .body("message", is(
                        "OClass contains one or more abac rules, links, datasets or namedqueries, remove them to delete the oClass %s"
                                .formatted(oClassDto.getName())));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { "class_write" })
    public void addClass_withUndefinedStorage_shouldThrow() {
        var field = testService.createAndSaveField();
        var id = UUID.randomUUID();
        var attributeDefDto1 = testService.createAttributeDto(id, "attribute1", "attribute1", field.id);
        var oClassDto = testService.createClassWriteDto(UUID.randomUUID(), "oclass", Storage.KUZZLE_ASSET, attributeDefDto1);
        oClass = mapper.toModel(oClassDto);

        assertThatThrownBy(() -> modelService.saveClass(oClassDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "Storage %s is not defined as a storage in your configuration".formatted(Storage.KUZZLE_ASSET));
    }
}
