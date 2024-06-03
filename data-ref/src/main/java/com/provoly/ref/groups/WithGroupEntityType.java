package com.provoly.ref.groups;

import java.util.UUID;

public enum WithGroupEntityType {
    DASHBOARD(DashboardGroupRelations::new),
    DATASET(DatasetGroupRelations::new),
    WIDGET(WidgetGroupRelations::new);

    private final GroupRelationsBuilder groupRelationsBuilder;

    WithGroupEntityType(GroupRelationsBuilder groupRelationsBuilder) {
        this.groupRelationsBuilder = groupRelationsBuilder;
    }

    @FunctionalInterface
    private interface GroupRelationsBuilder {
        public GroupRelations build(UUID id, Group group, UUID dashboardId, boolean canWrite);
    }

    public GroupRelations buildGroupRelations(UUID id, Group group, UUID dashboardId, boolean canWrite) {
        return this.groupRelationsBuilder.build(id, group, dashboardId, canWrite);
    }
}
