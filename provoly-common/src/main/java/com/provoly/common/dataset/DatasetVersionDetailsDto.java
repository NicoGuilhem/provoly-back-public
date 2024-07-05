package com.provoly.common.dataset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.provoly.common.metadata.MetadataValueReadDto;

public non-sealed class DatasetVersionDetailsDto extends DatasetVersionBaseDto {
    private List<MetadataValueReadDto> metadata;
    private boolean hasWarnings;
    private DatasetDetailsDto dataset;

    public DatasetVersionDetailsDto(UUID id, DatasetDetailsDto dataset, UUID oClass, Instant lastModified,
            Integer version, DatasetState state, String fileName,
            List<MetadataValueReadDto> metadata, Instant productionDate, String producer, String additionalInformation) {
        super(id, oClass, lastModified, version, state, fileName, productionDate, producer, additionalInformation);
        this.metadata = metadata != null ? metadata : new ArrayList<>();
        this.hasWarnings = false;
        this.dataset = dataset;
    }

    public boolean isHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public List<MetadataValueReadDto> getMetadata() {
        return metadata;
    }

    public DatasetDetailsDto getDataset() {
        return dataset;
    }
}
