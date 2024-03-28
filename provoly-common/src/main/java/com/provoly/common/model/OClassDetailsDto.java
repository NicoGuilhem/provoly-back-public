package com.provoly.common.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.Storage;
import com.provoly.common.metadata.MetadataValueReadDto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OClassDetailsDto {

    private final UUID id;
    private final String slug;
    private final String name;
    private final String icon;
    private final List<AttributeDefDetailsDto> attributes;
    private final Storage storage;
    private List<MetadataValueReadDto> metadata;

    @Default
    @JsonCreator
    public OClassDetailsDto(UUID id, String slug, String name, String icon, List<AttributeDefDetailsDto> attributes,
            Storage storage,
            List<MetadataValueReadDto> metadata) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.icon = icon;
        this.attributes = attributes;
        this.storage = storage == null ? Storage.ELASTIC : storage;
        this.metadata = metadata;
    }

    public OClassDetailsDto(UUID id, String name, List<AttributeDefDetailsDto> attributes, String slug, Storage storage) {
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.slug = slug;
        this.storage = storage;
        this.metadata = List.of();
        this.icon = "";
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public List<AttributeDefDetailsDto> getAttributes() {
        return attributes;
    }

    public Storage getStorage() {
        return storage;
    }

    public List<MetadataValueReadDto> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValueReadDto> metadata) {
        this.metadata = metadata;
    }

    public String getIcon() {
        return icon;
    }

    public Optional<AttributeDefDetailsDto> getAttributeById(UUID attributeId) {
        return this.getAttributes().stream()
                .filter(a -> a.id.equals(attributeId))
                .findAny();
    }

    public List<Optional<AttributeDefDetailsDto>> getAttributesByIds(List<UUID> attributesIds) {
        return attributesIds.stream().map(this::getAttributeById).toList();
    }
}