package com.provoly.ref.category;

import java.util.List;

import com.provoly.common.model.CategoryDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class })
public interface CategoryMapper {
    CategoryDto toDto(Category dto);

    List<CategoryDto> toCategoriesDtoList(List<Category> categories);
}
