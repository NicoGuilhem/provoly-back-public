package com.provoly.ref.model;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.OClassWriteDto;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class OClassMapper {

    @BeforeMapping
    void verifyAttributeTechnicalNames(OClassWriteDto oClassWriteDto, @MappingTarget OClass oClass) {
        oClassWriteDto.getAttributes()
                .stream()
                .filter(att -> att.getTechnicalName() == null)
                .findAny()
                .ifPresent(attribute -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Missing technical name in attribute %s".formatted(attribute.getId()));
                });
    }

    @AfterMapping
    void verifyDuplicateIdsInAttributeSet(OClassDetailsDto oClassDto, @MappingTarget OClass oClass) {
        if (oClassDto.getAttributes().size() != oClass.getAttributes().size()) {
            throw new BusinessException(ErrorCode.ID_ALREADY_USED, "Duplicate id in attributes");
        }
    }

    @AfterMapping
    void verifyDuplicateIdsInAttributeSet(OClassWriteDto oClassDto, @MappingTarget OClass oClass) {
        if (oClassDto.getAttributes().size() != oClass.getAttributes().size()) {
            throw new BusinessException(ErrorCode.ID_ALREADY_USED, "Duplicate id in attributes");
        }
    }

}
