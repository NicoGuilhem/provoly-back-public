package com.provoly.common.model;

import java.util.List;
import java.util.UUID;

import com.provoly.common.metadata.MetadataValueWriteDto;

public interface WithMetadata {
    List<MetadataValueWriteDto> getMetadata();

    void setMetadata(List<MetadataValueWriteDto> metadata);

    UUID getId();
}
