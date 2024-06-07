package com.provoly.ref.dashboard;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdService;

@ApplicationScoped
@Transactional
public class DashboardRepository {
    private EntityIdService entityIdService;

    public DashboardRepository(EntityIdService entityIdService) {
        this.entityIdService = entityIdService;
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
