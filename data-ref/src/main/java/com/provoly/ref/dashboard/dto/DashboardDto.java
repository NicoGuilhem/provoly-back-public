package com.provoly.ref.dashboard.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.common.Default;
import com.provoly.common.dataset.GroupRights;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DashboardDto {

    private UUID id;
    private String name;
    private String image;
    private String description;
    private boolean cover;
    private List<UUID> datasource;
    private Map<String, List<GroupRights>> accessRightsByGroup;
    private String additionalInformation;

    @Default
    @JsonCreator
    public DashboardDto(UUID id, String name, String image, String description, boolean cover,
            List<UUID> datasource, Map<String, List<GroupRights>> accessRightsByGroup, String additionalInformation) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.description = description;
        this.cover = cover;
        this.datasource = datasource;
        this.accessRightsByGroup = accessRightsByGroup;
        this.additionalInformation = additionalInformation;
    }

    public DashboardDto(UUID id, String name, String additionalInformation) {
        this(id, name, null, null, false, null, null, additionalInformation);
    }

    public DashboardDto(UUID id, String name, List<UUID> datasource, String additionalInformation) {
        this(id, name, null, null, false, datasource, null, additionalInformation);
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

    public Map<String, List<GroupRights>> getAccessRightsByGroup() {
        return accessRightsByGroup;
    }

    public void setAccessRightsByGroup(Map<String, List<GroupRights>> accessRightsByGroup) {
        this.accessRightsByGroup = accessRightsByGroup;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}
