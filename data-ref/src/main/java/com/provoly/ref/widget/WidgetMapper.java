package com.provoly.ref.widget;

import java.time.Instant;
import java.util.Collection;

import com.provoly.ref.model.EntityLoader;
import com.provoly.ref.widget.dto.WidgetDetailsDto;
import com.provoly.ref.widget.dto.WidgetWriteDto;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, WidgetGroupMapper.class })
public interface WidgetMapper {

    void update(WidgetWriteDto dto, @MappingTarget WidgetCatalog widgetCatalog);

    WidgetCatalog toEntity(WidgetWriteDto dto);

    WidgetDetailsDto toDetailsDto(WidgetCatalog widgetCatalog);

    Collection<WidgetDetailsDto> toCollectionWidgetDetailsDto(Collection<WidgetCatalog> widgetCatalogs);

    @AfterMapping
    default void setModificationDate(@MappingTarget WidgetCatalog model) {
        model.setModificationDate(Instant.now());
    }
}
