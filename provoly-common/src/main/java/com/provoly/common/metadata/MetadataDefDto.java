package com.provoly.common.metadata;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import com.provoly.common.VariableType;

public class MetadataDefDto {

    public UUID id;
    public String name;
    public VariableType type;
    public String description;
    public Collection<String> allowedValues;
    public String slug;
    public boolean readOnly;
    public boolean system;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetadataDefDto that = (MetadataDefDto) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
