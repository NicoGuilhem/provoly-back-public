package com.provoly.ref.dashboard.dto;

import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DashboardDto {

    private UUID id;
    private String name;
    private String image;
    private String description;
    private boolean cover;
    private List<UUID> datasource;
    private List<String> groups;

    @Default
    @JsonCreator
    public DashboardDto(UUID id, String name, String image, String description, boolean cover,
            List<UUID> datasource, List<String> groups) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.description = description;
        this.cover = cover;
        this.datasource = datasource;
        this.groups = groups;
    }

    public DashboardDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public DashboardDto(UUID id, String name, List<UUID> datasource) {
        this.id = id;
        this.name = name;
        this.datasource = datasource;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public boolean getCover() {
        return cover;
    }

    public List<UUID> getDatasource() {
        return datasource;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
