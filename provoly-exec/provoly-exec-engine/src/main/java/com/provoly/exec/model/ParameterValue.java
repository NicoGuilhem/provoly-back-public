package com.provoly.exec.model;

import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class ParameterValue {

    private String name;
    private String value; // TODO : Allow to store binary data

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParameterValue that = (ParameterValue) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String fileContent) {
        this.value = fileContent;
    }
}
