package com.provoly.ref.datasetversion;

import java.util.Collection;
import java.util.List;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, uses = {
        EntityLoader.class, EntitySlugMapper.class, DatasetVersionMetadataValueMapper.class,
        DatasetVersionDetailsMapper.class })
public interface DatasetVersionMapper {

    @Mapping(source = "dataset.oClass.id", target = "oClass")
    DatasetVersionDto toDto(DatasetVersion datasetVersion);

    @Mapping(source = "dataset.oClass.id", target = "oClass")
    DatasetVersionDetailsDto toDatasetVersionDetailsDto(DatasetVersion datasetVersion);

    Collection<DatasetVersionDetailsDto> toDatasetVersionDetailsDto(Collection<DatasetVersion> datasetVersions);

    @InheritInverseConfiguration
    @Mapping(target = "dataset", source = "dataset")
    DatasetVersion toModel(DatasetVersionDto dto);

    DatasetVersionMessage toDto(DatasetVersionMessage datasetVersionMessage);

    List<DatasetVersionMessageDto> toCollectionDto(Collection<DatasetVersionMessage> datasetVersionMessages);
}
