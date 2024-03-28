package com.provoly.transfo;

import java.util.Collection;

import com.provoly.EntityLoader;
import com.provoly.common.transfo.GuiDto;
import com.provoly.common.transfo.NodeDto;
import com.provoly.common.transfo.TransfoDetailsDto;
import com.provoly.common.transfo.TransfoDto;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", injectionStrategy = InjectionStrategy.CONSTRUCTOR, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        EntityLoader.class, NodeSpecMapper.class, LastExecutionMapper.class })
public interface TransfoMapper {
    Transfo toEntity(TransfoDto dto);

    TransfoDto toDto(Transfo entity);

    Collection<TransfoDetailsDto> toDetailsDto(Collection<Transfo> entity);

    Gui toGui(GuiDto gui);

    TransfoDetailsDto toDetailsDto(Transfo entity);

    @Mapping(source = "node", target = "spec", qualifiedByName = "jsonNodetoNodeSpec")
    NodeDto toDto(Node node);

    void update(TransfoDetailsDto detailsDto, @MappingTarget Transfo entity);
}
