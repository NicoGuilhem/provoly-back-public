package com.provoly.common.model;

import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.Storage;
import com.provoly.common.metadata.MetadataValueWriteDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OClassWriteDto implements WithMetadata {
    private final UUID id;
    private final String name;
    private final Storage storage;
    private final String icon;
    private final List<AttributeDefWriteDto> attributes;
    private final String slug;
    private List<MetadataValueWriteDto> metadata;

    @Default
    @JsonCreator
    public OClassWriteDto(UUID id, String name, String icon, List<AttributeDefWriteDto> attributes, String slug,
            Storage storage,
            List<MetadataValueWriteDto> metadata) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.attributes = attributes;
        this.slug = slug;
        this.storage = storage == null ? Storage.ELASTIC : storage;
        this.metadata = metadata;
    }

    public OClassWriteDto(UUID id, String name, List<AttributeDefWriteDto> attributes, Storage storage,
            List<MetadataValueWriteDto> metadata) {
        this(id, name, null, attributes, null, storage, metadata);
    }

    public OClassWriteDto(UUID id, String name, List<AttributeDefWriteDto> attributes) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.icon = null;
        this.slug = null;
        this.storage = Storage.ELASTIC;
        this.metadata = List.of();
    }

    public OClassWriteDto(UUID id, String name, List<AttributeDefWriteDto> attributes, Storage storage) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.icon = null;
        this.slug = null;
        this.storage = storage;
        this.metadata = List.of();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public List<AttributeDefWriteDto> getAttributes() {
        return attributes;
    }

    public String getSlug() {
        return slug;
    }

    public Storage getStorage() {
        return storage;
    }

    @Override
    public List<MetadataValueWriteDto> getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(List<MetadataValueWriteDto> metadata) {
        this.metadata = metadata;
    }
}
