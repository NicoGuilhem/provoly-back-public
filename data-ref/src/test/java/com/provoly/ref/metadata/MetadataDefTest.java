package com.provoly.ref.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.user.Role;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MetadataDefTest {

    @Inject
    MetadataDefController metadataDefController;

    @Inject
    MetadataDefService metadataDefService;

    private MetadataDefDto metadataDefDto;

    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    TestService testService;

    @BeforeEach
    public void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
    }

    @AfterEach
    public void clean() {
        metadataDefService.delete(metadataDefDto.id);
    }

    private void generateMetadataDefWithType(VariableType type, String description) {
        metadataDefDto = new MetadataDefDto();
        metadataDefDto.id = UUID.fromString("32c52b41-b197-4e57-8f6b-a8bf53c9c167");
        metadataDefDto.name = UUID.randomUUID().toString();
        metadataDefDto.type = type;
        metadataDefDto.description = description;
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public void simple_type_is_functional() {
        generateMetadataDefWithType(VariableType.STRING, "insert simple type");
        metadataDefController.addMetadata(metadataDefDto);
        assertThat(metadataDefController.getById(metadataDefDto.id)).isNotNull();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public void simple_type_with_allowed_values_is_ko() {
        generateMetadataDefWithType(VariableType.STRING, "insert simple type with allowedValues");
        metadataDefDto.allowedValues = Set.of("value1", "Value2");

        assertThatThrownBy(() -> metadataDefController.addMetadata(metadataDefDto)).asInstanceOf(type(BusinessException.class))
                .extracting(BusinessException::getStatus, BusinessException::getMessage)
                .containsExactly(Response.Status.BAD_REQUEST, "Only type LIST accept allowedValues");

    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public void type_list_is_ok() {
        generateMetadataDefWithType(VariableType.LIST, "insert type list with allowedValues");
        metadataDefDto.allowedValues = Set.of("value1", "Value2");
        metadataDefController.addMetadata(metadataDefDto);
        assertThat(metadataDefController.getById(metadataDefDto.id).allowedValues).containsExactlyInAnyOrder("value1",
                "Value2");
    }
}
