package com.provoly.ref.metadata;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class MetadataDefAllowedValue extends MetadataAllowedValue {

    @ManyToOne
    private MetadataDef metadataDef;

    protected MetadataDefAllowedValue() {
        super();
    }

    public MetadataDefAllowedValue(String value) {
        super(value);
    }

    void setMetadataDef(MetadataDef metadataDef) {
        this.metadataDef = metadataDef;
    }
}
