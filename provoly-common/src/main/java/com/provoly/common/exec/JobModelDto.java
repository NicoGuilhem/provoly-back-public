package com.provoly.common.exec;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

public class JobModelDto {
    private final UUID id;
    private final String image;
    private final Set<ParameterDto> parameters;

    public JobModelDto(UUID id, String image) {
        this(id, image, Collections.emptySet());
    }

    @Default // For mapstruct
    @JsonCreator
    public JobModelDto(UUID id, String image, Set<ParameterDto> parameters) {
        this.id = id;
        this.image = image;
        this.parameters = parameters == null ? Collections.emptySet() : Collections.unmodifiableSet(parameters);
    }

    @Override
    public String toString() {
        return "JobModelDto{" +
                "id=" + id +
                ", image='" + image + '\'' +
                ", parameters=" + parameters +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public String getImage() {
        return image;
    }

    public Set<ParameterDto> getParameters() {
        return parameters;
    }
}
