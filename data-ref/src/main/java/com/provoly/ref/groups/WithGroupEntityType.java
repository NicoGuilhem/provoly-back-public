package com.provoly.ref.groups;

import java.util.UUID;

import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.widget.WidgetCatalog;

public enum WithGroupEntityType {
    DASHBOARD(Dashboard.class, DashboardGroupRelations::new),
    DATASET(Dataset.class, DatasetGroupRelations::new),
    WIDGET(WidgetCatalog.class, WidgetGroupRelations::new);

    private final Class<? extends EntityId> entity;
    private final GroupRelationsBuilder groupRelationsBuilder;

    WithGroupEntityType(Class<? extends EntityId> entity, GroupRelationsBuilder groupRelationsBuilder) {
        this.entity = entity;
        this.groupRelationsBuilder = groupRelationsBuilder;
    }

    @FunctionalInterface
    private interface GroupRelationsBuilder {
        public GroupRelations build(UUID id, Group group, UUID dashboardId, boolean canWrite);
    }

    public GroupRelations buildGroupRelations(UUID id, Group group, UUID entityId, boolean canWrite) {
        return this.groupRelationsBuilder.build(id, group, entityId, canWrite);
    }

    public <T extends EntityId> Class<T> getEntity() {
        return (Class<T>) entity;
    }
}
