package com.provoly.exec.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

@Entity
public class JobModel extends EntityId {

    private String image;

    @ElementCollection
    private Set<Parameter> parameters = new HashSet<>();

    protected JobModel() {
    } // For JPA

    public JobModel(UUID id, String image) {
        super(id);
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public Set<Parameter> getParameters() {
        return parameters;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setParameters(Set<Parameter> parameters) {
        this.parameters = parameters;
    }
}
