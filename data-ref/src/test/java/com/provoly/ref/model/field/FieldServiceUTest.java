package com.provoly.ref.model.field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.Type;
import com.provoly.common.model.field.*;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.model.*;
import com.provoly.ref.user.VisibilityType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FieldServiceUTest {
    FieldService fieldService;
    FieldMapper fieldMapper;
    EntityIdRepository entityIdRepository;
    RefEventService refEventService;
    AssociationService associationService;
    FieldRepository fieldRepository;

    @BeforeEach
    public void init() {
        entityIdRepository = mock(EntityIdRepository.class);
        refEventService = mock(RefEventService.class);
        fieldMapper = mock(FieldMapper.class);
        associationService = mock(AssociationService.class);
        fieldRepository = mock(FieldRepository.class);
        fieldService = new FieldService(fieldMapper, entityIdRepository, refEventService, associationService, fieldRepository);
    }

    @Test
    void add_numeric_field_should_succeed() {
        FieldNumericDto fieldNumericDto = new FieldNumericDto(UUID.randomUUID(), "fieldNumeric", Type.INTEGER.getName(),
                "slug_numeric", false, "mètre");

        Assertions.assertDoesNotThrow(() -> fieldService.addField(fieldNumericDto));
    }

    @Test
    void update_numeric_field_should_succeed() {
        UUID id = UUID.randomUUID();
        FieldNumericDto fieldNumericDto = new FieldNumericDto(id, "fieldNumeric", Type.INTEGER.getName(), "slug_numeric", true,
                "mètre");
        FieldNumeric fieldNumeric = new FieldNumeric(id, "fieldNumeric", "slug_numeric", Type.INTEGER, true, "mètre");

        fieldService.addField(fieldNumericDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldNumeric);

        Assertions.assertDoesNotThrow(() -> fieldService.updateField(id, fieldNumericDto));
    }

    @Test
    void update_numeric_field_with_different_type_should_throw() {
        UUID id = UUID.randomUUID();
        FieldNumericDto fieldNumericDto = new FieldNumericDto(id, "fieldNumeric", Type.INTEGER.getName(), "slug_numeric", false,
                "mètre");
        FieldNumericDto fieldNumericDtoUpdated = new FieldNumericDto(id, "fieldNumeric", Type.STRING.getName(), "slug_numeric",
                false,
                "mètre");
        FieldNumeric fieldNumeric = new FieldNumeric(id, "fieldNumeric", "slug_numeric", Type.INTEGER, false, "mètre");

        fieldService.addField(fieldNumericDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldNumeric);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.updateField(id, fieldNumericDtoUpdated);
                }, "You're not allowed to change type %s to %s".formatted(fieldNumericDto.getType(),
                        fieldNumericDtoUpdated.getType()));
    }

    @Test
    void add_string_field_should_succeed() {
        FieldDto fieldStringDto = new FieldDto(UUID.randomUUID(), "fieldString", Type.STRING.getName(),
                "slug_numeric");

        Assertions.assertDoesNotThrow(() -> fieldService.addField(fieldStringDto));
    }

    @Test
    void add_an_existing_string_field_should_throw() {
        FieldDto fieldStringDto = new FieldDto(UUID.randomUUID(), "fieldString", Type.STRING.getName(),
                "slug_numeric");

        when(fieldRepository.fieldExists(fieldStringDto.getId())).thenReturn(true);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.addField(fieldStringDto);
                }, "Field %s already exists.".formatted(fieldStringDto.getId()));
    }

    @Test
    void add_string_field_with_same_name_should_throw() {
        FieldDto fieldStringDto = new FieldDto(UUID.randomUUID(), "fieldString", Type.STRING.getName(),
                "slug_string");
        Field fieldString = new Field(UUID.randomUUID(), "fieldString", "slug_string", Type.STRING);

        when(entityIdRepository.getById(any(), any())).thenReturn(fieldString);
        doThrow(new BusinessException(
                ErrorCode.NAME_ALREADY_USED, "Name %s already exists".formatted("name"))).when(entityIdRepository)
                .saveEntity(any());

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.addField(fieldStringDto);
                }, "Field '%s' already exists".formatted(fieldStringDto.getName()));
    }

    @Test
    void update_string_field_with_same_name_should_throw() {
        FieldDto fieldStringDto = new FieldDto(UUID.randomUUID(), "fieldString", Type.STRING.getName(),
                "slug_string");
        Field fieldString = new Field(UUID.randomUUID(), "fieldString", "slug_string", Type.STRING);

        when(entityIdRepository.getById(any(), any())).thenReturn(fieldString);
        doThrow(new BusinessException(
                ErrorCode.NAME_ALREADY_USED, "Name %s already exists".formatted("name"))).when(entityIdRepository)
                .saveEntity(any());

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.updateField(fieldStringDto.getId(), fieldStringDto);
                }, "Field '%s' already exists".formatted(fieldStringDto.getName()));
    }

    @Test
    void update_string_field_should_succeed() {
        UUID id = UUID.randomUUID();
        FieldDto fieldStringDto = new FieldDto(id, "fieldString", Type.STRING.getName(), "slug_numeric");
        Field fieldString = new Field(id, "fieldNumeric", "slug_numeric", Type.STRING);

        fieldService.addField(fieldStringDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldString);

        Assertions.assertDoesNotThrow(() -> fieldService.updateField(id, fieldStringDto));
    }

    @Test
    void add_decimal_field_should_succeed() {
        FieldDecimalDto fieldDecimalDto = new FieldDecimalDto(UUID.randomUUID(), "fieldString", "slug_numeric",
                Type.DECIMAL.getName(), 6, true, "mètre");

        Assertions.assertDoesNotThrow(() -> fieldService.addField(fieldDecimalDto));
    }

    @Test
    void update_decimal_field_should_succeed() {
        UUID id = UUID.randomUUID();
        FieldDecimalDto fieldDecimalDto = new FieldDecimalDto(UUID.randomUUID(), "fieldString", "slug_numeric",
                Type.DECIMAL.getName(), 6, true, "mètre");
        FieldDecimal fieldDecimal = new FieldDecimal(id, "fieldNumeric", "slug_numeric", Type.DECIMAL, 6, true, "mètre");

        fieldService.addField(fieldDecimalDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldDecimal);

        Assertions.assertDoesNotThrow(() -> fieldService.updateField(id, fieldDecimalDto));
    }

    @Test
    void add_date_field_should_succeed() {
        FieldDateDto fieldDateDto = new FieldDateDto(UUID.randomUUID(), "fieldString", Type.INSTANT.getName(), "slug_numeric",
                "MONTH");

        Assertions.assertDoesNotThrow(() -> fieldService.addField(fieldDateDto));
    }

    @Test
    void update_date_field_should_succeed() {
        UUID id = UUID.randomUUID();
        FieldDateDto fieldDateDto = new FieldDateDto(id, "fieldString", Type.INSTANT.getName(), "slug_numeric",
                "MONTH");
        FieldDate fieldDate = new FieldDate(id, "fieldNumeric", "slug_numeric", Type.INSTANT, "MONTH");

        fieldService.addField(fieldDateDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldDate);

        Assertions.assertDoesNotThrow(() -> fieldService.updateField(id, fieldDateDto));
    }

    @Test
    void add_geo_field_should_succeed() {
        FieldGeoDto fieldGeoDto = new FieldGeoDto(UUID.randomUUID(), "fieldString", "MULTILINESTRING", "slug_numeric",
                "EPSG:4326");

        Assertions.assertDoesNotThrow(() -> fieldService.addField(fieldGeoDto));
    }

    @Test
    void update_geo_field_should_succeed() {
        UUID id = UUID.randomUUID();
        FieldGeoDto fieldGeoDto = new FieldGeoDto(id, "fieldGeo", "MULTILINESTRING", "slug_geo", "EPSG:4326");
        FieldGeo fieldGeo = new FieldGeo(id, "fieldGeo", "slug_geo", "EPSG:4326", Type.MULTILINESTRING);

        fieldService.addField(fieldGeoDto);
        when(entityIdRepository.getById(any(), any())).thenReturn(fieldGeo);

        Assertions.assertDoesNotThrow(() -> fieldService.updateField(id, fieldGeoDto));
    }

    @Test
    void add_date_field_with_wrong_format_should_throw() {
        FieldDateDto fieldDateDto = new FieldDateDto(UUID.randomUUID(), "fieldDate", Type.INSTANT.getName(), "slug_date", "MO");

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.addField(fieldDateDto);
                }, "Format %s is not allowed.".formatted(fieldDateDto.getFormat()));
    }

    @Test
    void add_geo_field_with_wrong_crs_should_throw() {
        FieldGeoDto fieldGeoDto = new FieldGeoDto(UUID.randomUUID(), "fieldString", "MULTILINESTRING", "slug_numeric",
                "ESPG:4326");

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.addField(fieldGeoDto);
                }, "Field: %s have an incompatible CRS: %s".formatted(fieldGeoDto.getId(), fieldGeoDto.getCrs()));
    }

    @Test
    public void should_saveIfGeoFieldWithValidCrs() {
        UUID id = UUID.randomUUID();
        FieldGeo fieldGeo = new FieldGeo(id, "name", "slug", "ESPG:4326", Type.POINT);
        FieldGeoDto fieldGeoDto = new FieldGeoDto(id, "name", Type.POINT.getName(), "slug", "EPSG:4326");

        when(fieldMapper.toModel(fieldGeoDto)).thenReturn(fieldGeo);
        fieldService.addField(fieldGeoDto);
        verify(entityIdRepository).saveEntity(fieldGeo);
    }

    @Test
    void add_geo_field_with_null_crs_should_throw() {
        FieldGeoDto fieldGeoDto = new FieldGeoDto(UUID.randomUUID(), "fieldString", "MULTILINESTRING", "slug_numeric", null);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.addField(fieldGeoDto);
                }, "CRS required for geo field");
    }

    @Test
    void delete_a_string_field_with_association_should_throw() {
        FieldDto fieldStringDto = new FieldDto(UUID.randomUUID(), "fieldString", Type.STRING.getName(),
                "slug_numeric");
        AssociationsDto associationsDto = new AssociationsDto(List
                .of(new AssociationDto(UUID.randomUUID(), "association", VisibilityType.PRIVATE, AssociationsType.DASHBOARD)),
                true);

        when(fieldRepository.fieldExists(fieldStringDto.getId())).thenReturn(true);
        when(associationService.getFieldAssociations(fieldStringDto.getId())).thenReturn(associationsDto);

        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fieldService.deleteFieldById(fieldStringDto.getId());
                }, "The field %s is used by one or more attributes, remove them to delete the field"
                        .formatted(fieldStringDto.getId()));
    }
}
