package com.provoly.ref.model.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.search.FieldConditionDto;
import com.provoly.common.search.MultiClassRequestDto;
import com.provoly.common.search.NamedQueryDto;
import com.provoly.common.search.VisibilityDto;
import com.provoly.common.user.Role;
import com.provoly.ref.model.*;
import com.provoly.ref.user.NamedQueryService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FieldServiceTest {
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    FieldController fieldController;
    @Inject
    TestService testService;
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    NamedQueryService namedQueryService;
    @Inject
    Logger logger;
    @Inject
    AssociationService associationService;

    private final UUID nqPrivateId = UUID.randomUUID();
    private final UUID attributeId = UUID.randomUUID();
    private OClassWriteDto classDto = null;
    private AttributeDefDto attributeDefDto = new AttributeDefDto();
    private FieldDto fieldDto = null;

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        fieldDto = testService.createAndSaveField();
        attributeDefDto = testService.createAttributeDto(attributeId, "attributeName", "attributeId" + attributeId,
                fieldDto);
        classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));
    }

    @AfterEach
    @Transactional
    public void clear() {
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_FIELD_WRITE })
    public void deleteInexistantFieldThrowError404() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> fieldController.deleteFieldById(id))
                .isInstanceOf(ProvolyNotFoundException.class)
                .hasMessageContaining("Field : %s inexistant.".formatted(id));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_FIELD_WRITE })
    public void deleteFieldUsedByAttributeThrowError400() {
        assertThatThrownBy(() -> fieldController.deleteFieldById(fieldDto.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "The field %s is used by one or more attributes, remove them to delete the field"
                                .formatted(fieldDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void delete_Field_when_associateNamedQueryExist_should_throw_badRequest() {
        // init with field + attribute def + class

        namedQueryService.saveNamedQueryForUser(
                createMultiNamedQueryDto(nqPrivateId, "test NQ name", VisibilityType.PUBLIC, classDto, fieldDto.getId()));
        logger.debugf("Get namedquery as result : %s", namedQueryService.getById(nqPrivateId).toString());

        assertThatThrownBy(() -> fieldController.deleteFieldById(fieldDto.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "The field %s is used by one or more attributes, remove them to delete the field"
                                .formatted(fieldDto.getId()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_associationsOfField_returnOClassAssociated() {
        // init with field + attribute def + class
        AssociationsDto result = associationService.getFieldAssociations(fieldDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(classDto.getId()))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_associationsOfField_returnNamedQueryAssociated() {
        namedQueryService.saveNamedQueryForUser(
                createMultiNamedQueryDto(nqPrivateId, "test NQ name", VisibilityType.PUBLIC, classDto, fieldDto.getId()));
        logger.debugf("Get namedquery as result : %s", namedQueryService.getById(nqPrivateId).toString());

        AssociationsDto result = associationService.getFieldAssociations(fieldDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .containsExactlyInAnyOrder(nqPrivateId, classDto.getId())
                .hasSize(2);
    }

    private NamedQueryDto createMultiNamedQueryDto(UUID id, String name, VisibilityType visibilityType, OClassWriteDto classDto,
            UUID fieldId) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name(), List.of());
        return new NamedQueryDto(id, name, "description",
                new MultiClassRequestDto(List.of(classDto.getId()), List.of(new FieldConditionDto(fieldId, "test value"))),
                vis);
    }
}
