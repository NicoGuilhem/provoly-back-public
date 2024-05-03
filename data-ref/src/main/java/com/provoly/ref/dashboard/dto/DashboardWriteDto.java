package com.provoly.ref.dashboard.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.dataset.GroupRights;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.WithMetadata;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DashboardWriteDto extends DashboardDto implements WithMetadata {
    private Map<String, Object> manifest;
    private List<MetadataValueWriteDto> metadata = new ArrayList<>();

    @Default
    @JsonCreator
    public DashboardWriteDto(UUID id, String name, String image, String description, boolean cover,
            List<UUID> datasource, Map<String, Object> manifest, List<MetadataValueWriteDto> metadata,
            Map<String, List<GroupRights>> groups,
            String additionalInformation) {
        super(id, name, image, description, cover, datasource, groups, additionalInformation);
        this.manifest = manifest;
        this.metadata = metadata;
    }

    public DashboardWriteDto(UUID id, String name, String image, String description, boolean cover,
            List<UUID> datasource, Map<String, Object> manifest, List<MetadataValueWriteDto> metadata,
            Map<String, List<GroupRights>> groups) {
        this(id, name, image, description, cover, datasource, manifest, metadata, groups, null);
    }

    public DashboardWriteDto(UUID id, String name) {
        super(id, name, null);
    }

    public DashboardWriteDto(UUID id, String name, List<UUID> datasource) {
        super(id, name, datasource, null);
    }

    public DashboardWriteDto(UUID id, String name, List<UUID> datasource, String additionalInformation) {
        super(id, name, datasource, additionalInformation);
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
