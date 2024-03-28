package com.provoly.ref.model;

import java.util.*;

import jakarta.persistence.*;

import com.provoly.common.Default;
import com.provoly.common.Storage;
import com.provoly.ref.entity.EntitySlug;

@Entity
public class OClass extends EntitySlug {
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "oclass", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<AttributeDef> attributes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Storage storage;

    protected OClass() {
        super();
    }

    @Default
    public OClass(UUID id) {
        super(id);
    }

    public void addAttribute(AttributeDef attribute) {
        attributes.add(attribute);
        attribute.setClass(this);
    }

    public void deleteAttribute(AttributeDef attributeDef) {
        attributes.remove(attributeDef);
    }

    public Optional<AttributeDef> getAttributeById(UUID id) {
        return attributes.stream().filter(attr -> id.equals(attr.getId())).findFirst();
    }

    @Override
    public String toString() {
        return "OClass{" +
                "attributes=" + attributes +
                ", slug='" + slug + '\'' +
                ", name='" + name + '\'' +
                ", id=" + id +
                "} ";
    }

    public Set<AttributeDef> getAttributes() {
        return attributes;
    }

    public Storage getStorage() {
        return this.storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
