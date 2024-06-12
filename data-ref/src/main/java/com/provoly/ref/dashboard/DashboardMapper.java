package com.provoly.ref.dashboard;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import com.provoly.ref.dashboard.dto.DashboardReadDto;
import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.user.ProvolyUser;

import org.mapstruct.*;

@Mapper(componentModel = "jakarta", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED, uses = {
        DashboardMetadataMapper.class, DashboardGroupMapper.class })
public interface DashboardMapper {

    Dashboard toModel(DashboardWriteDto dashboard, @Context ProvolyUser user);

    @AfterMapping
    default void convert(@MappingTarget Dashboard dashboard, @Context ProvolyUser user) {
        dashboard.setUser(user);
    }

    @Mapping(source = "owner", target = "owner", ignore = true)
    DashboardReadDto toReadDto(Dashboard dashboard);

    List<DashboardReadDto> toReadDto(List<Dashboard> dashboard);

    @AfterMapping
    default void fillUrl(Dashboard model, @MappingTarget DashboardReadDto dashboardReadDto) {
        URI uri = UriBuilder.fromResource(DashboardController.class)
                .path(DashboardController.class, "getDashBoardManifest")
                .resolveTemplateFromEncoded("id", model.getId()).build();
        dashboardReadDto.setManifestUrl(uri);
    }

    @AfterMapping
    default void setModificationDate(@MappingTarget Dashboard model) {
        model.setModificationDate(Instant.now());
    }

    void update(DashboardWriteDto dto, @MappingTarget Dashboard model);
}
