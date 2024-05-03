package com.provoly.ref.dashboard.dto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.dataset.GroupRights;
import com.provoly.common.metadata.MetadataValueReadDto;

public class DashboardReadDto extends DashboardDto {

    private Instant creationDate;
    private Instant modificationDate;
    private URI manifestUrl;
    private List<MetadataValueReadDto> metadata;
    private List<MetadataValueReadDto> readOnlyMetadata;
    private boolean owner;

    @Default
    public DashboardReadDto(UUID id, String name,
            String image,
            String description,
            boolean cover,
            List<MetadataValueReadDto> metadata,
            List<UUID> datasource,
            Instant creationDate,
            Instant modificationDate,
            URI manifestUrl,
            Map<String, List<GroupRights>> groups, boolean owner, String additionalInformation) {
        super(id, name, image, description, cover, datasource, groups, additionalInformation);
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
        this.manifestUrl = manifestUrl;
        this.metadata = metadata;
        this.owner = owner;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public Instant getModificationDate() {
        return modificationDate;
    }

    public URI getManifestUrl() {
        return manifestUrl;
    }

    public void setManifestUrl(URI manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public List<MetadataValueReadDto> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValueReadDto> metadata) {
        this.metadata = metadata;
    }

    public List<MetadataValueReadDto> getReadOnlyMetadata() {
        return readOnlyMetadata;
    }

    public void setReadOnlyMetadata(List<MetadataValueReadDto> readOnlyMetadata) {
        this.readOnlyMetadata = readOnlyMetadata;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}
