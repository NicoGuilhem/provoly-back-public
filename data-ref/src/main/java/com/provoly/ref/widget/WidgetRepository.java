package com.provoly.ref.widget;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.user.ProvolyUser;

@ApplicationScoped
@Transactional
public class WidgetRepository {
    private EntityIdService entityIdService;
    private EntityManager em;

    public WidgetRepository(EntityIdService entityIdService, EntityManager em) {
        this.entityIdService = entityIdService;
        this.em = em;
    }

    public List<WidgetCatalog> getAllowedWidgets(ProvolyUser user) {
        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT widget_catalog.id FROM widget_catalog " +
                        "LEFT JOIN group_relations as gr ON widget_catalog.id = gr.entity_id " +
                        "LEFT JOIN group_def as gd ON gr.group_id = gd.id " +
                        "WHERE gd.name in :groups_names OR widget_catalog.user_id = :user_id ) "
                        + "SELECT * FROM widget_catalog where id in (SELECT id FROM ids)",
                WidgetCatalog.class)
                .setParameter("user_id", user.getId())
                .setParameter("groups_names", user.getGroups().stream().map(EntityNamed::getName).toList())
                .getResultList();
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
