package com.provoly.ref.model;

import java.util.Collection;

import com.provoly.common.model.*;
import com.provoly.common.model.field.*;
import com.provoly.ref.model.field.*;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, subclassExhaustiveStrategy = SubclassExhaustiveStrategy.COMPILE_ERROR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class })
public interface FieldMapper {

    // Field
    @SubclassMapping(source = FieldNumericDto.class, target = FieldNumeric.class)
    @SubclassMapping(source = FieldGeoDto.class, target = FieldGeo.class)
    @SubclassMapping(source = FieldDecimalDto.class, target = FieldDecimal.class)
    @SubclassMapping(source = FieldDateDto.class, target = FieldDate.class)
    Field toModel(FieldDto dto);

    Collection<Field> toModel(Collection<FieldDto> fields);

    @SubclassMapping(source = FieldDecimal.class, target = FieldDecimalDto.class)
    @SubclassMapping(source = FieldNumeric.class, target = FieldNumericDto.class)
    @SubclassMapping(source = FieldGeo.class, target = FieldGeoDto.class)
    @SubclassMapping(source = FieldDate.class, target = FieldDateDto.class)
    FieldDto toDto(Field field);

    Collection<FieldDto> toFieldDto(Collection<Field> dto);
}
