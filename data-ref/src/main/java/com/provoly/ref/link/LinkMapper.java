package com.provoly.ref.link;

import java.util.Collection;

import com.provoly.common.link.LinkDetailsDto;
import com.provoly.common.link.LinkDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, EntitySlugMapper.class })
public interface LinkMapper {

    LinkDetailsDto toDto(Link link);

    Collection<LinkDetailsDto> toClassDto(Collection<Link> dto);

    @InheritInverseConfiguration
    Link toModel(LinkDto dto);
}
