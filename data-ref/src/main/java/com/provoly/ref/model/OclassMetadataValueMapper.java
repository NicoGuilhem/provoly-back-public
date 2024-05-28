package com.provoly.ref.model;

import java.util.List;

import jakarta.inject.Inject;

import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.OClassReadDto;
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
        oClassDetailsDto.setMetadata(getMetadataValuesReadDto(oClass));
    }

    @AfterMapping
    void metadataValueReadDtoToMetadataValue(OClass oClass, @MappingTarget OClassReadDto oClassReadDto) {
        oClassReadDto.setMetadata(getMetadataValuesReadDto(oClass));
    }

    private List<MetadataValueReadDto> getMetadataValuesReadDto(OClass oClass) {
        var metadataValues = metadataService.getMetadataValueByEntityId(oClass.getId());
        return metadataValues.stream()
                .map(mv -> new MetadataValueReadDto(mv.getValue(),
                        mapper.toDto(metadataDefService.getById(mv.getMetadataDefId()))))
                .toList();
    }
}
