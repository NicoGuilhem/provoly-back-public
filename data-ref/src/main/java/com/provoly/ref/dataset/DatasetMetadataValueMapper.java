package com.provoly.ref.dataset;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.metadata.MetadataService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DatasetMetadataValueMapper {

    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetadataService metadataService;
    @Inject
    MetadataMapper mapper;

    @AfterMapping
    void metadataValueReadDtoToMetadataValue(
            Dataset dataset, @MappingTarget DatasetDetailsDto datasetDetailsDto) {
        var metadataValues = metadataService.getMetadataValueByEntityId(dataset.getId());

        var metadataValuesReadDto = metadataValues.stream()
                .map(mv -> new MetadataValueReadDto(mv.getValue(),
                        mapper.toDto(metadataDefService.getById(mv.getMetadataDefId()))))
                .toList();

        datasetDetailsDto.getMetadata().addAll(metadataValuesReadDto);
    }

}
