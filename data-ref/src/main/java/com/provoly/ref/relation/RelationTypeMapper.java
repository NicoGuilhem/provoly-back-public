package com.provoly.ref.relation;

import java.util.Collection;

import com.provoly.common.relation.RelationTypeDetailsDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, RelationTypeDetailsMapper.class, EntitySlugMapper.class })
public interface RelationTypeMapper {

    @Mapping(target = "slug", ignore = true)
    void update(RelationTypeDto dto, @MappingTarget RelationType relationType);

    RelationTypeDetailsDto toDtoDetails(RelationType relationType);

    Collection<RelationTypeDto> toClassDto(Collection<RelationType> dto);

    @InheritInverseConfiguration
    RelationType toModel(RelationTypeDto dto);

}
