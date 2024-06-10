package com.provoly.ref.widget;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
@Transactional
public class WidgetRepository {
    private EntityIdRepository entityIdRepository;

    public WidgetRepository(EntityIdRepository entityIdRepository) {
        this.entityIdRepository = entityIdRepository;
    }

    public WidgetCatalog getWidgetCatalogById(UUID id) {
        return entityIdRepository.getById(id, WidgetCatalog.class);
    }

    public void removeEntity(UUID id) {
        entityIdRepository.removeEntity(id, WidgetCatalog.class);
    }

    public Collection<WidgetCatalog> getAll() {
        return entityIdRepository.getAll(WidgetCatalog.class);
    }

    public WidgetCatalog findById(UUID id) {
        return entityIdRepository.findById(id, WidgetCatalog.class);
    }

    public void saveWidget(WidgetCatalog widgetCatalog) {
        entityIdRepository.saveEntity(widgetCatalog);
    }
}
