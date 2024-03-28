package com.provoly.exec.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Parameter {

    private String name;
    private String filename; // Only if type is file

    protected Parameter() {
    } // For JPA

    public Parameter(String name) {
        this.name = name;
    }

    public Parameter(String name, String filename) {
        this.name = name;
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String path) {
        this.filename = path;
    }
}
