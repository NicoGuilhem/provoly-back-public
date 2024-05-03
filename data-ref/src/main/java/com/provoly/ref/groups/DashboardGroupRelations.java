package com.provoly.ref.groups;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.provoly.common.Default;

import com.fasterxml.jackson.annotation.JsonCreator;

@Entity
@DiscriminatorValue("DASHBOARD")
public class DashboardGroupRelations extends GroupRelations {

    @JsonCreator
    @Default
    public DashboardGroupRelations(UUID id, Group group, UUID dashboardId, boolean canWrite) {
        super(id, WithGroupEntityType.DASHBOARD, group, dashboardId, canWrite);
    }

    public DashboardGroupRelations() {
    }
}
