package com.provoly.ref.metadata;

import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class MetadataAllowedValue {
    @Id
    @GeneratedValue
    private UUID id;

    private String value;

    protected MetadataAllowedValue() {
    }

    public MetadataAllowedValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UUID getId() {
        return id;
    }
}
