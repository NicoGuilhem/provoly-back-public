package com.provoly.ref.abac;

import java.util.Collection;
import java.util.UUID;

import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.ContextVariableDto;
import com.provoly.common.abac.PredicateDto;
import com.provoly.ref.abac.predicate.Predicate;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.model.AttributeDef;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.model.EntitySlugMapper;
import com.provoly.ref.user.SearchMapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, MetadataMapper.class, SearchMapper.class, EntitySlugMapper.class })
public interface AbacMapper {

    PredicateDto toDto(Predicate predicate);

    Collection<PredicateDto> toCollectionPredicateDto(Collection<Predicate> predicates);

    @InheritInverseConfiguration
    Predicate toModel(PredicateDto predicateDto);

    AbacRuleDto toDto(AbacRule rule);

    Collection<AbacRuleDto> toRuleDto(Collection<AbacRule> rules);

    @InheritInverseConfiguration
    AbacRule toModel(AbacRuleDto dto);

    default UUID toUuid(AttributeDef attribute) {
        return attribute.getId();
    }

    ContextVariableDto toDto(ContextVariable abacContextVariable);

    Collection<ContextVariableDto> toCollectionAbacVariableContextDto(Collection<ContextVariable> abacContextVariables);

    @InheritInverseConfiguration
    ContextVariable toModel(ContextVariableDto abacContextVariableDto);

}
