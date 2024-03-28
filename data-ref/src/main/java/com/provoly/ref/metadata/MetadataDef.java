package com.provoly.ref.metadata;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.VariableType;
import com.provoly.ref.entity.EntitySlug;

@Entity
public class MetadataDef extends EntitySlug {

    @Enumerated(EnumType.STRING)
    VariableType type;

    String description;

    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER, mappedBy = "metadataDef", orphanRemoval = true)
    private Set<MetadataDefAllowedValue> values = new HashSet<>();

    private boolean readOnly = false;
    private boolean system = false;

    protected MetadataDef() {
        super();
    }

    @Default
    public MetadataDef(UUID id, String name, VariableType type, String description, String slug) {
        super(id);
        this.name = name;
        this.type = type;
        this.description = description;
        this.slug = slug;
    }

    public void addValue(MetadataDefAllowedValue value) {
        value.setMetadataDef(this);
        values.add(value);
    }

    public Set<MetadataDefAllowedValue> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetadataDef that = (MetadataDef) o;
        return Objects.equals(id, that.getId());
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isSystem() {
        return system;
    }
}
