package com.provoly.ref.datasetversion;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.imports.MessageLevel;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DatasetVersionDetailsMapper {
    @Inject
    DatasetVersionRepository datasetVersionRepository;

    @AfterMapping
    void setHasWarnings(@MappingTarget DatasetVersionDetailsDto datasetVersionDetailsDto) {
        boolean hasWarnings = datasetVersionRepository.countDatasetVersionMessagesByLevel(datasetVersionDetailsDto.getId(),
                MessageLevel.WARNING) > 0;
        datasetVersionDetailsDto.setHasWarnings(hasWarnings);
    }
}
