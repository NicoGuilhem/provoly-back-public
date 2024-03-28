package com.provoly.common.search;

import java.util.List;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VisibilityDto {
    private final String type;
    private final List<UUID> users;

    @Default
    @JsonCreator
    public VisibilityDto(String type, List<UUID> users) {
        this.type = type;
        this.users = users;
    }

    public VisibilityDto(String type) {
        this.type = type;
        this.users = null;
    }

    public String getType() {
        return type;
    }

    public List<UUID> getUsers() {
        return users;
    }
}
