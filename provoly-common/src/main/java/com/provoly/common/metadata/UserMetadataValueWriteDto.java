package com.provoly.common.metadata;

import java.util.List;
import java.util.UUID;

public class UserMetadataValueWriteDto {

    private List<String> values;
    private UUID metadataDefId;

    public UserMetadataValueWriteDto() {
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public UUID getMetadataDefId() {
        return metadataDefId;
    }

    public void setMetadataDefId(UUID metadataDefId) {
        this.metadataDefId = metadataDefId;
    }
}
