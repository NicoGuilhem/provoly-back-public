package com.provoly.ref.abac;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.VariableType;
import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.MetadataConditionDto;
import com.provoly.common.search.Operator;
import com.provoly.common.user.Role;
import com.provoly.ref.abac.predicate.Predicate;
import com.provoly.ref.abac.predicate.PredicateService;
import com.provoly.ref.metadata.MetadataDef;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.model.ModelMapper;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AbacControllerTest {
    final UUID id = UUID.fromString("a6af868b-623d-4564-932b-e534a2323eae");
    final UUID predId = UUID.fromString("73e2b450-e200-4eff-9dff-894f7dd21e12");
    @Inject
    PredicateService predicateService;
    @Inject
    MetadataDefService metadataDefService;
    @Inject
    TestService testService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        Predicate pred = new Predicate(predId);
        pred.setName("predicate");
        pred.setValue("value");
        predicateService.save(pred);
        metadataDefService.addMetadata(new MetadataDef(id, "meta", VariableType.STRING, "", "slug"));
    }

    @AfterEach
    @Transactional
    public void clean() {
        testService.clean();
    }

    private AbacRuleDto createRuleAttr(UUID predId) {
        AbacRuleDto rule = new AbacRuleDto();
        rule.name = "rule";
        rule.id = UUID.randomUUID();
        rule.type = AbacRuleType.ATTRIBUTE;
        rule.predicate = predId;
        rule.condition = new AttributeConditionDto(id, "value", Operator.EXISTS);
        return rule;
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE })
    public void addRule_noPred_return_400() {
        UUID predId = UUID.randomUUID();
        given()
                .body(createRuleAttr(predId))
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(404)
                .body("message", is("Predicate : " + predId + " inexistant."));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE })
    public void addRule_noAttr_return_400() {
        AbacRuleDto rule = createRuleAttr(predId);

        given()
                .body(rule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(404)
                .body("message", is("AttributeDef : a6af868b-623d-4564-932b-e534a2323eae inexistant."));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE })
    public void addRule_metadata_return_200() {
        AbacRuleDto rule = new AbacRuleDto();
        rule.name = "rule";
        rule.id = UUID.randomUUID();
        rule.type = AbacRuleType.METADATA;
        rule.predicate = predId;
        rule.condition = new MetadataConditionDto(id, "toto", Operator.EXISTS);

        given()
                .body(rule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE })
    public void addRule_attribute_whithMetadataCondition_return_400() {
        AbacRuleDto rule = new AbacRuleDto();
        rule.name = "rule";
        rule.id = UUID.randomUUID();
        rule.type = AbacRuleType.ATTRIBUTE;
        rule.predicate = predId;
        rule.condition = new MetadataConditionDto(id, "toto", Operator.EXISTS);

        given()
                .body(rule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(400)
                .body("message", is("RuleType ATTRIBUTE does not allowed Metadata condition"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE })
    public void addRule_attribute_return_204() {
        OClassWriteDto oClass = initClass();

        AbacRuleDto attributeRule = new AbacRuleDto();
        attributeRule.name = "rule";
        attributeRule.id = UUID.randomUUID();
        attributeRule.type = AbacRuleType.ATTRIBUTE;
        attributeRule.predicate = predId;
        attributeRule.condition = new AttributeConditionDto(oClass.getAttributes().getFirst().getId(), "toto", Operator.EQUALS);

        given()
                .body(attributeRule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_WRITE, Role.STR_DATA_ACCESS_READ })
    public void getRules_forClass_shouldRetrieveAttributeRules_butNotRetrieveMetadataRules() {
        OClassWriteDto oClass = initClass();

        AbacRuleDto attributeRule = new AbacRuleDto();
        attributeRule.name = "attributeRule";
        attributeRule.id = UUID.randomUUID();
        attributeRule.type = AbacRuleType.ATTRIBUTE;
        attributeRule.active = true;
        attributeRule.predicate = predId;
        attributeRule.condition = new AttributeConditionDto(oClass.getAttributes().getFirst().getId(), "toto", Operator.EQUALS);

        given()
                .body(attributeRule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(204);

        AbacRuleDto metadataRule = new AbacRuleDto();
        metadataRule.name = "metadataRule";
        metadataRule.id = UUID.randomUUID();
        metadataRule.type = AbacRuleType.METADATA;
        metadataRule.active = true;
        metadataRule.predicate = predId;
        metadataRule.condition = new MetadataConditionDto(id, "toto", Operator.EXISTS);

        given()
                .body(metadataRule)
                .contentType(ContentType.JSON)
                .when()
                .post("abac/rules")
                .then()
                .statusCode(204);

        given()
                .pathParam("oClassId", oClass.getId())
                .contentType(ContentType.JSON)
                .when()
                .get("abac/rules/class/{oClassId}")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].type", is("ATTRIBUTE"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_READ })
    public void getRule_return_404() {
        given()
                .pathParam("id", id.toString())
                .contentType(ContentType.JSON)
                .when()
                .get("abac/rules/id/{id}")
                .then()
                .statusCode(404)
                .body("message", is("AbacRule : a6af868b-623d-4564-932b-e534a2323eae inexistant."));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DATA_ACCESS_READ })
    public void getRule_noOclass_return_404() {
        given()
                .pathParam("id", id.toString())
                .contentType(ContentType.JSON)
                .when()
                .get("abac/rules/class/{id}")
                .then()
                .statusCode(400)
                .body("message", is("OClass : a6af868b-623d-4564-932b-e534a2323eae inexistant."));
    }

    private OClassWriteDto initClass() {
        UUID fieldId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        FieldDto fieldDto = testService.createAndSaveField(fieldId);
        AttributeDefDto attributeDefDto = testService.createAttributeDto(attributeId, "attributeName",
                "attributeId" + attributeId, fieldDto);
        OClassWriteDto classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));
        return classDto;
    }
}
