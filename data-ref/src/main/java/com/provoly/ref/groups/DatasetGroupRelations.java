package com.provoly.ref.groups;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("DATASET")
public class DatasetGroupRelations extends GroupRelations {

    @JsonCreator
    @Default
    public DatasetGroupRelations(UUID id, UUID groupId, UUID dataset) {
        super(id, WithGroupEntityType.DATASET, groupId, dataset);
    }

    public DatasetGroupRelations() {
    }
}
