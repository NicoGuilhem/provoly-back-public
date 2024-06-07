package com.provoly.ref.widget;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
@Transactional
public class WidgetRepository {
    private EntityIdService entityIdService;

    public WidgetRepository(EntityIdService entityIdService) {
        this.entityIdService = entityIdService;
    }

    public WidgetCatalog getWidgetCatalogById(UUID id) {
        return entityIdService.getById(id, WidgetCatalog.class);
    }

    public void removeEntity(UUID id) {
        entityIdService.removeEntity(id, WidgetCatalog.class);
    }

    public Collection<WidgetCatalog> getAll() {
        return entityIdService.getAll(WidgetCatalog.class);
    }

    public WidgetCatalog findById(UUID id) {
        return entityIdService.findById(id, WidgetCatalog.class);
    }

    public void saveWidget(WidgetCatalog widgetCatalog) {
        entityIdService.saveEntity(widgetCatalog);
    }
}
