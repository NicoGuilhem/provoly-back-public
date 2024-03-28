package com.provoly.ref.widget;

import java.time.Instant;
import java.util.Collection;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.widget.dto.WidgetDetailsDto;
import com.provoly.ref.widget.dto.WidgetDto;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class })
public interface WidgetMapper {

    @Mapping(source = "visibility.type", target = "visibilityType")
    void update(WidgetDto dto, @MappingTarget WidgetCatalog widgetCatalog);

    @Mapping(source = "visibility.type", target = "visibilityType")
    WidgetCatalog toEntity(WidgetDto dto);

    @Mapping(source = "widgetCatalog.visibilityType", target = "visibility.type")
    WidgetDetailsDto toDto(WidgetCatalog widgetCatalog, ProvolyUserWidgetCatalog user);

    @AfterMapping
    default void setModificationDate(@MappingTarget WidgetCatalog model) {
        model.setModificationDate(Instant.now());
    }

    default Collection<WidgetDetailsDto> toCollectionWidgetDetailsDto(Collection<ProvolyUserWidgetCatalog> entity) {
        return entity.stream()
                .map(wc -> toDto(wc.getWidgetCatalog(), wc))
                .toList();
    }

    default VisibilityType map(String visibilityType) {
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
