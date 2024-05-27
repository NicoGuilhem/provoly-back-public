package com.provoly.ref.widget.dto;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class WidgetDetailsDto {
    private UUID id;
    private String name;
    private String description;
    private String image;
    private String content;
    private Collection<UUID> datasource;
    private boolean cover;
    private List<String> groups;
    private boolean owner;
    private Instant creationDate;
    private Instant modificationDate;

    @Default
    @JsonCreator
    public WidgetDetailsDto(UUID id, String name, String description, String image, String content, Collection<UUID> datasource,
            boolean cover, List<String> groups, boolean owner, Instant creationDate,
            Instant modificationDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = image;
        this.content = content;
        this.datasource = datasource;
        this.cover = cover;
        this.groups = groups;
        this.owner = owner;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
    }

    protected WidgetDetailsDto() {
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public Instant getModificationDate() {
        return modificationDate;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getContent() {
        return content;
    }

    public Collection<UUID> getDatasource() {
        return datasource;
    }

    public boolean isCover() {
        return cover;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
