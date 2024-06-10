package com.provoly.ref.dashboard;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.ref.entity.EntityIdRepository;

@ApplicationScoped
@Transactional
public class DashboardRepository {
    private EntityIdRepository entityIdRepository;

    public DashboardRepository(EntityIdRepository entityIdRepository) {
        this.entityIdRepository = entityIdRepository;
    }

    public Dashboard findById(UUID dashboardId) {
        return entityIdRepository.findById(dashboardId, Dashboard.class);
    }

    public Dashboard getDashboard(UUID id) {
        return entityIdRepository.getById(id, Dashboard.class);
    }

    public Collection<Dashboard> getAll() {
        return entityIdRepository.getAll(Dashboard.class);
    }

    public void save(Dashboard dashboard) {
        entityIdRepository.saveEntity(dashboard);
    }

    public void delete(UUID id) {
        entityIdRepository.removeEntity(id, Dashboard.class);
    }
}
