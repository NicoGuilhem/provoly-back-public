package com.provoly.ref.dataset;

import java.util.Collection;

import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class, DatasetMetadataValueMapper.class, DatasetGroupMapper.class,
        DatasetCategoryMapper.class })
public interface DatasetMapper {

    DatasetDto toDto(Dataset dataset);

    @Mapping(source = "owner", target = "owner", ignore = true)
    DatasetDetailsDto toDatasetDetailsDto(Dataset dataset);

    Collection<DatasetDetailsDto> toDatasetDetailsDtoList(Collection<Dataset> dataset);

    @InheritInverseConfiguration
    Dataset toModel(DatasetDto dto);
}
