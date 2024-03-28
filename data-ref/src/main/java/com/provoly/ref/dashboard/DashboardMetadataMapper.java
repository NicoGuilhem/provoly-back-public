package com.provoly.ref.dashboard;

import jakarta.inject.Inject;

import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.metadata.MetadataService;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public abstract class DashboardMetadataMapper {

    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetadataService metadataService;

    @Inject
    MetadataMapper mapper;

    @AfterMapping
    void metadataValueReadDtoToMetadataValue(Dashboard dashboard, @MappingTarget DashboardReadDto dashboardReadDto) {
        var metadataValues = metadataService.getMetadataValueByEntityId(dashboard.getId());

        var metadataValuesReadDto = metadataValues.stream()
                .map(mv -> new MetadataValueReadDto(mv.getValue(),
                        mapper.toDto(metadataDefService.getById(mv.getMetadataDefId()))))
                .toList();

        dashboardReadDto.setMetadata(metadataValuesReadDto);
    }

}
