package com.provoly.ref.user;

import java.util.Collection;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.NamedQueryDetailsDto;
import com.provoly.common.search.NamedQueryDto;
import com.provoly.ref.model.EntityLoader;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, SearchMapper.class })
public interface NamedQueryMapper {

    @Mapping(source = "visibility.type", target = "visibilityType")
    @Mapping(target = "request", ignore = true)
    void update(NamedQueryDto dto, @MappingTarget NamedQuery namedQuery);

    @Mapping(source = "visibility.type", target = "visibilityType")
    NamedQuery toEntity(NamedQueryDto dto);

    @Mapping(source = "namedQuery.visibilityType", target = "visibility.type")
    NamedQueryDetailsDto toDto(NamedQuery namedQuery, ProvolyUserNamedQuery user);

    default Collection<NamedQueryDetailsDto> toNamedQueryDto(Collection<ProvolyUserNamedQuery> entity) {
        return entity.stream().map(nq -> toDto(nq.getNamedQuery(), nq)).toList();
    }

    default public VisibilityType map(String visibilityType) {
        try {
            if (visibilityType == null) {
                visibilityType = "PRIVATE";
            }
            return VisibilityType.valueOf(visibilityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Visibility type %s not valid".formatted(visibilityType));
        }
    }

}
