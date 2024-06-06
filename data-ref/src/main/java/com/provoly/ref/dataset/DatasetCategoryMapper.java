package com.provoly.ref.dataset;

import jakarta.inject.Inject;

import com.provoly.ref.category.CategoryMapper;
import com.provoly.ref.category.CategoryRepository;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DatasetCategoryMapper {
    @Inject
    CategoryRepository categoryRepository;
    @Inject
    CategoryMapper categoryMapper;

    @AfterMapping
    void getDatasetCategory(Dataset dataset, @MappingTarget DatasetDetailsDto datasetDto) {
        datasetDto.setCategories(categoryRepository.getCategoriesByEntityId(dataset.getId())
                .stream()
                .map(categoryRelations -> categoryMapper.toDto(categoryRelations.getCategory()))
                .toList());
    }
}