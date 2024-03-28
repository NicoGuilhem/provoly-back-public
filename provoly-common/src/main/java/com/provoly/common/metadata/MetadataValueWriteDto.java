package com.provoly.common.metadata;

import java.util.UUID;

public class MetadataValueWriteDto {

    private String value;
    private UUID metadataDefId;

    public MetadataValueWriteDto() {
    }

    public MetadataValueWriteDto(String value, UUID metadataDefId) {
        this.value = value;
        this.metadataDefId = metadataDefId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UUID getMetadataDefId() {
        return metadataDefId;
    }

    public void setMetadataDefId(UUID metadataDefId) {
        this.metadataDefId = metadataDefId;
    }
}
