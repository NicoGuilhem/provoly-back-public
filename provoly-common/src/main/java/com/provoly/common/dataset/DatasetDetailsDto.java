package com.provoly.common.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.provoly.common.metadata.MetadataValueReadDto;
import com.provoly.common.model.CategoryDto;

public class DatasetDetailsDto {

    private UUID id;
    private String name;
    private UUID oClass;
    private DatasetType type;
    private String description;
    private List<MetadataValueReadDto> metadata;
    private List<String> groups;
    private boolean owner;
    private String slug;
    private List<CategoryDto> categories;
    private DatasetVersionDto activeVersion;

    public DatasetDetailsDto(UUID id,
            String name,
            UUID oClass,
            DatasetType type,
            List<MetadataValueReadDto> metadata,
            String description,
            List<String> groups,
            boolean owner,
            String slug,
            List<CategoryDto> categories) {
        this.id = id;
        this.name = name;
        this.oClass = oClass;
        this.type = type;
        this.description = description;
        this.metadata = metadata == null ? new ArrayList<>() : metadata;
        this.groups = groups;
        this.owner = owner;
        this.slug = slug;
        this.categories = categories;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<MetadataValueReadDto> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValueReadDto> metadata) {
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

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<CategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
    }

    public DatasetVersionDto getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(DatasetVersionDto activeVersion) {
        this.activeVersion = activeVersion;
    }
}