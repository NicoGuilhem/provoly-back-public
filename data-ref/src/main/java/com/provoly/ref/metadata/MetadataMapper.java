package com.provoly.ref.metadata;

import java.util.List;
import java.util.Set;

import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class })
public interface MetadataMapper {

    @Mapping(source = "values", target = "allowedValues")
    MetadataDefDto toDto(MetadataDef metadata);

    Set<MetadataDefDto> toMetadataDto(List<MetadataDef> metadata);

    @Mapping(source = "allowedValues", target = "values")
    MetadataDef toModel(MetadataDefDto dto);

    default MetadataDefAllowedValue toModel(String allowedValue) {
        return new MetadataDefAllowedValue(allowedValue);
    }

    default String toDto(MetadataDefAllowedValue value) {
        return value.getValue();
    }
}
