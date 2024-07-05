package com.provoly.ref.model;

import java.util.Collection;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.*;
import com.provoly.common.model.field.*;
import com.provoly.common.search.FieldConditionDto;
import com.provoly.ref.category.Category;
import com.provoly.ref.model.field.*;
import com.provoly.ref.model.field.FieldGeo;
import com.provoly.ref.model.field.FieldNumeric;
import com.provoly.ref.searchrequest.FieldCondition;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, subclassExhaustiveStrategy = SubclassExhaustiveStrategy.COMPILE_ERROR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class, OClassMapper.class, OclassMetadataValueMapper.class,
        AttributeCategoryMapper.class })
public interface ModelMapper {

    // OClass
    OClassDetailsDto toDetailsDto(OClass aClass);

    Collection<OClassDetailsDto> toClassDetailsDto(Collection<OClass> dto);

    Collection<OClassReadDto> toClassReadDto(Collection<OClass> dto);

    OClassReadDto toClassReadDto(OClass dto);

    @InheritInverseConfiguration
    OClass toModel(OClassDetailsDto dto);

    @InheritInverseConfiguration
    OClass toModel(OClassWriteDto dto);

    // Category
    CategoryDto toDetailsDto(Category category);

    Collection<CategoryDto> toCategoryDto(Collection<Category> dto);

    Category toModel(CategoryDto dto);

    // Field
    @SubclassMapping(source = FieldNumericDto.class, target = FieldNumeric.class)
    @SubclassMapping(source = FieldGeoDto.class, target = FieldGeo.class)
    @SubclassMapping(source = FieldDecimalDto.class, target = FieldDecimal.class)
    @SubclassMapping(source = FieldDateDto.class, target = FieldDate.class)
    Field toModel(FieldDto dto);

    Collection<Field> toModel(Collection<FieldDto> fields);

    @SubclassMapping(source = FieldNumeric.class, target = FieldNumericDto.class)
    @SubclassMapping(source = FieldGeo.class, target = FieldGeoDto.class)
    @SubclassMapping(source = FieldDecimal.class, target = FieldDecimalDto.class)
    @SubclassMapping(source = FieldDate.class, target = FieldDateDto.class)
    FieldDto toDto(Field field);

    Collection<FieldDto> toFieldDto(Collection<Field> dto);

    // AttributeDef
    AttributeDefDto toDefDto(AttributeDefDetailsDto dto);

    AttributeDefDetailsDto toDefDetails(AttributeDef attributeDef);

    AttributeDef toModel(AttributeDefDto dto);

    AttributeDef toModel(AttributeDefDetailsDto dto);

    @InheritInverseConfiguration
    AttributeDefDto toDto(AttributeDef field);

    FieldCondition toModel(FieldConditionDto dto);

    @InheritInverseConfiguration
    FieldConditionDto toDto(FieldCondition model);

    default String map(Type type) {
        return type.getName();
    }

    default Type map(String name) {
        try {
            return Type.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Type " + name + " not valid");
        }
    }

}
