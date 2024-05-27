package com.provoly.ref.groups;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("WIDGET")
public class WidgetGroupRelations extends GroupRelations {

    @JsonCreator
    @Default
    public WidgetGroupRelations(UUID id, Group group, UUID groupId, boolean canWrite) {
        super(id, WithGroupEntityType.WIDGET, group, groupId, canWrite);
    }

    public WidgetGroupRelations() {
    }
}
