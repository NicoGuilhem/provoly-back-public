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
    public DashboardGroupRelations(UUID id, UUID groupId, UUID dashboardId) {
        super(id, WithGroupEntityType.DASHBOARD, groupId, dashboardId);
    }

    public DashboardGroupRelations() {
    }
}
