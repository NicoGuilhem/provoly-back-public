package com.provoly.common.search;

import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class NamedQueryDto {

    private final UUID id;
    private final String name;
    private final String description;
    private final SearchRequestDto request;
    // Following attribute belong to the link between NamedQuery and User
    private final boolean favorite;
    private final String color;
    private final VisibilityDto visibility;

    @Default
    @JsonCreator
    public NamedQueryDto(UUID id, String name, String description, SearchRequestDto request, boolean favorite, String color,
            VisibilityDto visibility) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.request = request;
        this.favorite = favorite;
        this.color = color;
        this.visibility = visibility;
    }

    public NamedQueryDto(UUID id, String name, String description, SearchRequestDto request, VisibilityDto visibility) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.request = request;
        this.visibility = visibility;
        this.favorite = false;
        this.color = null;
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

    public SearchRequestDto getRequest() {
        return request;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getColor() {
        return color;
    }

    public VisibilityDto getVisibility() {
        return visibility;
    }

    public String getVisibilityType() {
        if (visibility != null) {
            return visibility.getType();
        }
        return null;
    }
}
