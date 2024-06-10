package com.provoly.ref.model;

import static com.provoly.common.error.ErrorCode.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.event.RefEventService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelServiceUnitTest {

    ModelService modelService;

    EntityIdRepository entityIdRepository;

    RefEventService refEventService;
    ModelMapper modelMapper;

    @BeforeEach
    public void init() {
        entityIdRepository = mock(EntityIdRepository.class);
        refEventService = mock(RefEventService.class);
        modelMapper = mock(ModelMapper.class);
        modelService = new ModelService(null, modelMapper, refEventService, null, entityIdRepository, null, null, null);
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
        verify(entityIdRepository).saveEntity(field);
    }

    @Test
    void saveClass_should_throw_if_duplicate_technicalName() {
        AttributeDef firstAttribute = new AttributeDef(UUID.randomUUID());
        firstAttribute.setName("nom");
        firstAttribute.setTechnicalName("technicalName");

        AttributeDef secondAttribute = new AttributeDef(UUID.randomUUID());
        secondAttribute.setName("nom");
        secondAttribute.setTechnicalName("technicalName");

        var oclass = new OClass();
        oclass.addAttribute(firstAttribute);
        oclass.addAttribute(secondAttribute);

        when(modelMapper.toModel(any(OClassWriteDto.class))).thenReturn(oclass);

        var error = Assertions.assertThrows(BusinessException.class,
                () -> modelService.saveClass(new OClassWriteDto(UUID.randomUUID(), "name", List.of())));
        assertThat(error.getCode()).isEqualTo(BAD_REQUEST);
    }

}
