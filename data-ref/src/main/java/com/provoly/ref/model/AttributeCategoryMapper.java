package com.provoly.ref.model;

import jakarta.inject.Inject;

import com.provoly.common.model.OClassReadDto;
import com.provoly.ref.category.CategoryRepository;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class AttributeCategoryMapper {
    @Inject
    CategoryRepository categoryRepository;

    @AfterMapping
    void getAttributeCategory(OClass oClass, @MappingTarget OClassReadDto oClassReadDto) {
        oClassReadDto.getAttributes()
                .forEach(attributeDefDto -> categoryRepository.getCategoriesByEntityId(attributeDefDto.getId())
                        .stream()
                        .map(category -> category.getCategory().getId())
                        .findFirst()
                        .ifPresent(attributeDefDto::setCategory));
    }
}