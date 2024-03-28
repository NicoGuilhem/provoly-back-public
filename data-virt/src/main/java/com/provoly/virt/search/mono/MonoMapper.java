package com.provoly.virt.search.mono;

import java.util.*;

import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.*;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.virt.entity.*;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MonoMapper {

    @Inject
    ObjectMapper mapper = new ObjectMapper();

    ItemsSearchResultDto toDto(ItemsSearchResult itemsSearchResult);

    ItemDto toDto(Item item);

    default UUID map(OClassDetailsDto value) {
        return value == null ? null : value.getId();
    }

    default String toDto(ItemId value) {
        return value.getAsString();
    }

    default Map<UUID, List<ItemDto>> map(Iterable<Item> value) {
        var items = new HashMap<UUID, List<ItemDto>>();
        value.forEach(
                item -> items.computeIfAbsent(item.getoClass().getId(), uuid -> new ArrayList<>())
                        .add(toDto(item)));
        return items;
    }

    default AttributeDto toDto(AttributeValue entity) {
        String className = entity.getClass().getSimpleName();
        switch (className) {
            case "AttributeSimpleValue":
                return toDto((AttributeSimpleValue) entity);
            case "AttributeMultiValue":
                return toDto((AttributeMultiValue) entity);
            default:
                throw new BusinessException(ErrorCode.TECHNICAL, "Unknown attribute type " + className);
        }
    }

    AttributeSimpleValueDto toDto(AttributeSimpleValue value);

    AttributeMultiValueDto toDto(AttributeMultiValue value);

    List<ItemDto> toDto(List<Item> item);

    Collection<AttributeSimpleValueDto> toSimpleValueDto(Iterable<AttributeSimpleValue> value);

    default String map(UUID value) {
        return value.toString();
    }

    default Map<String, Object> mapMetadataValue(Iterable<MetadataValueDto> value) {
        if (!value.iterator().hasNext()) {
            return null;
        }
        var result = new HashMap<String, Object>();
        for (MetadataValueDto metadataValueDto : value) {
            result.put(metadataValueDto.getName(), metadataValueDto.getValue());
        }
        return result;
    }

    default String map(SearchAfterContext value) {
        String searchAfterEncoded;
        if (value == null) {
            return null;
        }
        try {
            String searchAfterStringify = mapper.writeValueAsString(value);
            searchAfterEncoded = Base64.getEncoder().encodeToString(searchAfterStringify.getBytes());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Cannot serialize SearchAfterContext into string");
        }
        return searchAfterEncoded;
    }

    default SearchAfterContext map(String value) {
        SearchAfterContext searchAfterContext;
        try {
            String searchAfterContextDecoded = new String(Base64.getDecoder().decode(value));
            searchAfterContext = mapper.readValue(searchAfterContextDecoded, SearchAfterContext.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Cannot deserialize search after into SearchAfterContext");
        }
        return searchAfterContext;
    }
}
