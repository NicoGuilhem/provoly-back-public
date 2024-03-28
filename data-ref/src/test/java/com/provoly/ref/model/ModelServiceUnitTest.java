package com.provoly.ref.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.event.RefEventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelServiceUnitTest {

    ModelService modelService;

    EntityIdService entityIdService;

    RefEventService refEventService;
    ModelMapper modelMapper;

    @BeforeEach
    public void init() {
        entityIdService = mock(EntityIdService.class);
        refEventService = mock(RefEventService.class);
        modelMapper = mock(ModelMapper.class);
        modelService = new ModelService(null, modelMapper, refEventService, null, entityIdService, null, null);
    }

    @Test
    public void should_throwExceptionIfGeoFieldWithoutCrs() {
        FieldDto field = new FieldDto();
        field.type = Type.POINT.getName();
        Collection<FieldDto> fields = List.of(field);
        assertThatThrownBy(() -> modelService.addFields(fields))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CRS required for geo field");
    }

    @Test
    public void should_throwExceptionIfNotGeoFieldWithCrs() {
        FieldDto fieldDto = new FieldDto();
        fieldDto.type = Type.INSTANT.getName();
        fieldDto.crs = "ESPG:4326";
        Collection<FieldDto> fields = List.of(fieldDto);
        assertThatThrownBy(() -> modelService.addFields(fields))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CRS should be blank for non geo field");
    }

    @Test
    public void should_throwExceptionIfGeoFieldWithInvalidCrs() {
        FieldDto field = new FieldDto();
        field.type = Type.POINT.getName();
        field.crs = "4326";
        Collection<FieldDto> fields = List.of(field);
        assertThatThrownBy(() -> modelService.addFields(fields))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Field: " + field + " have an incompatible CRS: 4326");
    }

    @Test
    public void should_saveIfGeoFieldWithValidCrs() {
        Field field = new Field(UUID.randomUUID());
        field.setType(Type.POINT);
        field.setCrs("ESPG:4326");
        FieldDto fieldDto = new FieldDto();
        fieldDto.type = Type.POINT.getName();
        fieldDto.crs = "EPSG:4326";
        when(modelMapper.toModel(eq(fieldDto))).thenReturn(field);
        Collection<FieldDto> fields = List.of(fieldDto);
        modelService.addFields(fields);
        verify(entityIdService).saveEntity(field);
    }

}
