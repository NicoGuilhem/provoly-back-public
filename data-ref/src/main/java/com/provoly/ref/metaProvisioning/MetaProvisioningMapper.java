package com.provoly.ref.metaProvisioning;

import java.util.Collection;
import java.util.List;

import com.provoly.common.metadata.MetaProvisioningDto;
import com.provoly.common.metadata.MetaProvisioningReaderDto;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;
import com.provoly.ref.user.metadata.UserProfileMapper;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, MetadataMapper.class, UserProfileMapper.class, EntitySlugMapper.class })
public interface MetaProvisioningMapper {
    void update(MetaProvisioningDto dto, @MappingTarget MetaProvisioning metaProvisioning);

    MetaProvisioningDto toDto(MetaProvisioning metaProvisioning);

    MetaProvisioningReaderDto toDtoReader(MetaProvisioning metaProvisioning);

    List<MetaProvisioningReaderDto> toDtoReaderList(List<MetaProvisioning> metaProvisioning);

    Collection<MetaProvisioningDto> toMetaProvisioningDto(Collection<MetaProvisioning> dto);

    @InheritInverseConfiguration
    MetaProvisioning toModel(MetaProvisioningDto dto);
}
