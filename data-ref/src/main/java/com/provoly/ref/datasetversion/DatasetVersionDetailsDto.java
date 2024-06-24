package com.provoly.ref.datasetversion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.metadata.MetadataValueReadDto;

public class DatasetVersionDetailsDto extends DatasetVersionDto {
    private List<MetadataValueReadDto> metadata;
    private boolean hasWarnings;

    public DatasetVersionDetailsDto(UUID id, UUID dataset, UUID oClass, Instant lastModified,
            Integer version, DatasetState state, String fileName,
            List<MetadataValueReadDto> metadata, Instant productionDate, String producer, String additionalInformation) {
        super(id, dataset, oClass, lastModified, version, state, fileName, productionDate, producer, additionalInformation);
        this.metadata = metadata != null ? metadata : new ArrayList<>();
        this.hasWarnings = false;
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
}
