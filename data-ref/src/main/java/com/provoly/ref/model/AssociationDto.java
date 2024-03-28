package com.provoly.ref.model;

import java.util.UUID;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import com.provoly.ref.user.VisibilityType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AssociationDto {
    @Id
    private UUID id;
    private String name;
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private VisibilityType visibilityType;
    @Enumerated(EnumType.STRING)
    private AssociationsType type;

    public AssociationDto(UUID id, String name, VisibilityType visibilityType, AssociationsType type) {
        this.id = id;
        this.name = name;
        this.visibilityType = visibilityType;
        this.type = type;
    }

    public AssociationDto(UUID id, String name, String visibilityType, String type) {
        this.id = id;
        this.name = name;
        this.visibilityType = VisibilityType.valueOf(visibilityType);
        this.type = AssociationsType.valueOf(type);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VisibilityType getVisibilityType() {
        return visibilityType;
    }

    public void setVisibilityType(VisibilityType visibilityType) {
        this.visibilityType = visibilityType;
    }

    public AssociationsType getType() {
        return type;
    }

    public void setType(AssociationsType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "id = %s; name = %s; visibilityType = %s; type = %s;".formatted(id, name, visibilityType, type);
    }
}