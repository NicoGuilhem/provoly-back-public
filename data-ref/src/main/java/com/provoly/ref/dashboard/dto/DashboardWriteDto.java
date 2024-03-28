package com.provoly.ref.dashboard.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.WithMetadata;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DashboardWriteDto extends DashboardDto implements WithMetadata {
    private Map<String, Object> manifest;
    private List<MetadataValueWriteDto> metadata = new ArrayList<>();

    @Default
    @JsonCreator
    public DashboardWriteDto(UUID id, String name, String image, String description, boolean cover,
            List<UUID> datasource, Map<String, Object> manifest, List<MetadataValueWriteDto> metadata, List<String> groups) {
        super(id, name, image, description, cover, datasource, groups);
        this.manifest = manifest;
        this.metadata = metadata;
    }

    public DashboardWriteDto(UUID id, String name) {
        super(id, name);
    }

    public DashboardWriteDto(UUID id, String name, List<UUID> datasource) {
        super(id, name, datasource);
    }

    public Map<String, Object> getManifest() {
        return manifest;
    }

    @Override
    public List<MetadataValueWriteDto> getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(List<MetadataValueWriteDto> metadata) {
        this.metadata = metadata;
    }

    @Override
    public UUID getId() {
        return super.getId();
    }
}
