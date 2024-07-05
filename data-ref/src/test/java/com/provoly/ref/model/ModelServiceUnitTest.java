package com.provoly.ref.model;

import static com.provoly.common.error.ErrorCode.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.event.RefEventService;
import com.provoly.ref.model.field.FieldService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelServiceUnitTest {

    ModelService modelService;

    EntityIdRepository entityIdRepository;

    RefEventService refEventService;
    ModelMapper modelMapper;
    FieldService fieldService;

    @BeforeEach
    public void init() {
        entityIdRepository = mock(EntityIdRepository.class);
        refEventService = mock(RefEventService.class);
        modelMapper = mock(ModelMapper.class);
        fieldService = mock(FieldService.class);
        modelService = new ModelService(null, modelMapper, refEventService, null, entityIdRepository, null, null, null);
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
