package com.provoly.common.dataset;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.metadata.MetadataValueWriteDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DatasetDto {

    private UUID id;
    private String name;
    private UUID oClass;
    private DatasetType type;
    private String description;
    private List<MetadataValueWriteDto> metadata;
    private Collection<String> groups;

    @Default
    @JsonCreator
    public DatasetDto(UUID id, String name, UUID oClass, DatasetType type, String description,
            List<MetadataValueWriteDto> metadata, Collection<String> groups) {
        this.id = id;
        this.name = name;
        this.oClass = oClass;
        this.type = type;
        this.description = description;
        this.metadata = metadata;
        this.groups = groups;
    }

    public DatasetDto(UUID id, String name, UUID oClass, DatasetType type) {
        this(id, name, oClass, type, null, List.of(), null);
    }

    public DatasetDto(UUID id, String name, UUID oClass, DatasetType type, String description) {
        this(id, name, oClass, type, description, List.of(), null);
    }

    public DatasetDto(UUID id, String name, UUID oClass, DatasetType type,
            Collection<String> groups) {
        this(id, name, oClass, type, null, List.of(), groups);
    }

    public List<MetadataValueWriteDto> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValueWriteDto> metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DatasetType getType() {
        return type;
    }

    public void setType(DatasetType type) {
        this.type = type;
    }

    public UUID getoClass() {
        return oClass;
    }

    public void setoClass(UUID oClass) {
        this.oClass = oClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Collection<String> getGroups() {
        return groups;
    }

    public void setGroups(Collection<String> groups) {
        this.groups = groups;
    }
}
