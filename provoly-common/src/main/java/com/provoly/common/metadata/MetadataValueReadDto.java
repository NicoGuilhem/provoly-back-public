package com.provoly.common.metadata;

public final class MetadataValueReadDto {

    private String value;

    private MetadataDefDto metadataDef;

    public MetadataValueReadDto(String value, MetadataDefDto metadataDef) {
        this.value = value;
        this.metadataDef = metadataDef;
    }

    public String getValue() {
        return value;
    }

    public MetadataDefDto getMetadataDef() {
        return metadataDef;
    }

    public String toString() {
        return String.format("name: %s - value: %s", metadataDef.name, value);
    }
}
