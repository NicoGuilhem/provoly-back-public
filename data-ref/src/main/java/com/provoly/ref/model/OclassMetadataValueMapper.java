package com.provoly.ref.model;

import jakarta.inject.Inject;

import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.metadata.MetadataService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class OclassMetadataValueMapper {

    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetadataService metadataService;
    @Inject
    MetadataMapper mapper;

    @AfterMapping
    void metadataValueReadDtoToMetadataValue(OClass oClass, @MappingTarget OClassDetailsDto oClassDetailsDto) {
        var metadataValues = metadataService.getMetadataValueByEntityId(oClass.getId());

        var metadataValuesReadDto = metadataValues.stream()
                .map(mv -> new MetadataValueReadDto(mv.getValue(),
                        mapper.toDto(metadataDefService.getById(mv.getMetadataDefId()))))
                .toList();

        oClassDetailsDto.setMetadata(metadataValuesReadDto);
    }

}
