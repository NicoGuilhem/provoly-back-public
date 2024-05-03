package com.provoly.ref.dashboard;

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
public class DashboardRepository {
    private EntityManager em;
    private EntityIdService entityIdService;

    public DashboardRepository(EntityManager em, EntityIdService entityIdService) {
        this.em = em;
        this.entityIdService = entityIdService;
    }

    public List<Dashboard> getUserVisibleDashboards(ProvolyUser user) {
        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT dashboard.id FROM dashboard " +
                        "LEFT JOIN group_relations as gr ON dashboard.id = gr.entity_id " +
                        "LEFT JOIN group_def as gd ON gr.group_id = gd.id " +
                        "WHERE gd.name in :groups_names OR dashboard.user_id = :user_id ) "
                        + "SELECT * FROM dashboard where id in (SELECT id FROM ids)",
                Dashboard.class)
                .setParameter("user_id", user.getId())
                .setParameter("groups_names", user.getGroups().stream().map(EntityNamed::getName).toList())
                .getResultList();
    }

    public Dashboard findById(UUID dashboardId) {
        return entityIdService.findById(dashboardId, Dashboard.class);
    }

    public Dashboard getDashboard(UUID id) {
        return entityIdService.getById(id, Dashboard.class);
    }

    public Collection<Dashboard> getAll() {
        return entityIdService.getAll(Dashboard.class);
    }

    public void save(Dashboard dashboard) {
        entityIdService.saveEntity(dashboard);
    }

    public void delete(UUID id) {
        entityIdService.removeEntity(id, Dashboard.class);
    }
}
