package com.provoly.ref.dataset;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;

import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.Dashboard_;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.entity.EntityNamed_;
import com.provoly.ref.model.OClass_;
import com.provoly.ref.widget.WidgetCatalog;
import com.provoly.ref.widget.WidgetCatalog_;

@ApplicationScoped
@Transactional
public class DatasetRepository {
    private EntityIdRepository entityIdRepository;
    private EntityManager em;

    public DatasetRepository(EntityIdRepository entityIdRepository, EntityManager em) {
        this.entityIdRepository = entityIdRepository;
        this.em = em;
    }

    public void save(Dataset dataset) {
        entityIdRepository.saveEntity(dataset);
    }

    public void save(Dataset dataset, boolean checkNameDuplicate) {
        entityIdRepository.saveEntity(dataset, checkNameDuplicate);
    }

    public void delete(UUID datasetId) {
        entityIdRepository.removeEntity(datasetId, Dataset.class);
    }

    public boolean isNameAlreadyExistForClass(Dataset dataset) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var metadataRoot = q.from(Dataset.class);
        q.select(cb.count(metadataRoot));
        q.where(
                cb.equal(metadataRoot.get(Dataset_.O_CLASS), dataset.getoClass()),
                cb.equal(metadataRoot.get(EntityNamed_.NAME), dataset.getName()));
        return em.createQuery(q).getSingleResult() > 0;
    }

    public boolean exists(Dataset dataset) {
        return entityIdRepository.exists(dataset);
    }

    public List<Dataset> getAllForClass(UUID classId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Dataset.class);
        var root = q.from(Dataset.class);
        q = q.where(cb.equal(root.get(Dataset_.oClass).get(OClass_.id), classId));
        return em.createQuery(q).getResultList();
    }

    public Optional<Dataset> getByName(String name) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Dataset.class);
        var root = q.from(Dataset.class);
        q = q.where(cb.equal(root.get(Dataset_.name), name));
        return em.createQuery(q)
                .setMaxResults(1)
                .getResultStream()
                .findAny();
    }

    public Optional<Dataset> getById(UUID datasetId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Dataset.class);
        var root = q.from(Dataset.class);
        q = q.where(cb.equal(root.get(Dataset_.id), datasetId));
        return em.createQuery(q)
                .setMaxResults(1)
                .getResultStream()
                .findAny();
    }

    public Collection<UUID> getAllFilterByDatasource(Collection<UUID> datasource) {
        //We need to filter the datasource on dataset because groups are implemented only on dataset
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(Dataset.class);
        Path<UUID> idPath = root.get(Dataset_.ID);
        q = q.select(idPath);
        q = q.where(cb.in(root.get(Dataset_.ID)).value(datasource));
        return em.createQuery(q).getResultList();
    }

    public List<Dashboard> findAssociatedDashboards(UUID datasetId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Dashboard> criteriaQuery = cb.createQuery(Dashboard.class);
        Root<Dashboard> dashboardRoot = criteriaQuery.from(Dashboard.class);
        Join<Dashboard, UUID> dataSourceJoin = dashboardRoot.join(Dashboard_.datasource);
        Predicate dataSourceIdPredicate = cb.equal(dataSourceJoin, datasetId);
        criteriaQuery.select(dashboardRoot)
                .where(dataSourceIdPredicate);
        return em.createQuery(criteriaQuery).getResultList();
    }

    public List<WidgetCatalog> findAssociatedWidget(UUID datasetId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<WidgetCatalog> criteriaQuery = cb.createQuery(WidgetCatalog.class);
        Root<WidgetCatalog> widgetCatalogRoot = criteriaQuery.from(WidgetCatalog.class);
        Join<WidgetCatalog, UUID> dataSourceJoin = widgetCatalogRoot.join(WidgetCatalog_.datasource);
        Predicate dataSourceIdPredicate = cb.equal(dataSourceJoin, datasetId);
        criteriaQuery.select(widgetCatalogRoot)
                .where(dataSourceIdPredicate);
        return em.createQuery(criteriaQuery).getResultList();
    }

    public List<Dataset> getAll() {
        return entityIdRepository.getAll(Dataset.class);
    }
}
