package com.provoly.ref.user;

import com.provoly.common.search.*;
import com.provoly.ref.metadata.MetadataCondition;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.searchrequest.*;
import com.provoly.ref.searchrequest.Condition;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, uses = {
        EntityLoader.class, SortMapper.class })
public interface SearchMapper {

    @SubclassMapping(source = TrueConditionDto.class, target = TrueCondition.class)
    @SubclassMapping(source = AndConditionDto.class, target = AndCondition.class)
    @SubclassMapping(source = OrConditionDto.class, target = OrCondition.class)
    @SubclassMapping(source = AttributeConditionDto.class, target = AttributeCondition.class)
    @SubclassMapping(source = MetadataConditionDto.class, target = MetadataCondition.class)
    Condition map(ConditionDto dto);

    @SubclassMapping(source = TrueCondition.class, target = TrueConditionDto.class)
    @SubclassMapping(source = AndCondition.class, target = AndConditionDto.class)
    @SubclassMapping(source = OrCondition.class, target = OrConditionDto.class)
    @SubclassMapping(source = AttributeCondition.class, target = AttributeConditionDto.class)
    @SubclassMapping(source = MetadataCondition.class, target = MetadataConditionDto.class)
    ConditionDto map(Condition condition);

    @SubclassMapping(source = MonoClassRequestDto.class, target = MonoClassSearchRequest.class)
    @SubclassMapping(source = MultiClassRequestDto.class, target = MultiClassSearchRequest.class)
    SearchRequest toEntity(SearchRequestDto dto);

    @SubclassMapping(source = MonoClassSearchRequest.class, target = MonoClassRequestDto.class)
    @SubclassMapping(source = MultiClassSearchRequest.class, target = MultiClassRequestDto.class)
    SearchRequestDto toDto(SearchRequest entity);
}
