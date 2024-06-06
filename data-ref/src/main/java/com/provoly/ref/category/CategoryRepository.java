package com.provoly.ref.category;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityNamed_;

@ApplicationScoped
@Transactional
public class CategoryRepository {
    private EntityIdService entityIdService;
    @PersistenceContext
    private EntityManager em;

    public CategoryRepository(EntityIdService entityIdService, EntityManager em) {
        this.entityIdService = entityIdService;
        this.em = em;
    }

    public void checkNameAlreadyExists(Category category) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var metadataRoot = q.from(Category.class);
        q.select(cb.count(metadataRoot));
        q.where(cb.and(
                cb.equal(metadataRoot.get(EntityNamed_.name), category.getName()),
                cb.equal(metadataRoot.get(Category_.withCategoryEntityType), category.getWithCategoryEntityType())));
        var result = em.createQuery(q).getSingleResult();
        if (result > 0) {
            throw new BusinessException(ErrorCode.NAME_ALREADY_USED,
                    "%s category name already used".formatted(category.getName()));
        }
    }

    public void save(Category category) {
        entityIdService.saveEntity(category);
    }

    public List<Category> getAll(WithCategoryEntityType withCategoryEntityType) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Category.class);
        var root = q.from(Category.class);
        q.where(cb.equal(root.get(Category_.withCategoryEntityType), withCategoryEntityType));
        return em.createQuery(q).getResultList();
    }

    public List<CategoryRelations> getCategoriesByEntityId(UUID entityId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CategoryRelations.class);
        var rootQuery = q.from(CategoryRelations.class);
        q.where(cb.equal(rootQuery.get(CategoryRelations_.entityId), entityId));
        return em.createQuery(q).getResultList();
    }

    public void deleteCategoriesRelationsFromEntity(List<UUID> categoryRelationsIds) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createCriteriaDelete(CategoryRelations.class);
        var rootQuery = q.from(CategoryRelations.class);
        q.where(rootQuery.get(CategoryRelations_.id).in(categoryRelationsIds));
        em.createQuery(q).executeUpdate();
    }

    public void saveCategoryRelations(Category category, UUID entityId) {
        entityIdService.saveEntity(new CategoryRelations(UUID.randomUUID(), category, entityId));
    }

    public Category getById(UUID id) {
        return entityIdService.getById(id, Category.class);
    }

    public Category findById(UUID id) {
        return entityIdService.findById(id, Category.class);
    }

    public void deleteAllByEntityId(UUID entityId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createCriteriaDelete(CategoryRelations.class);
        var rootQuery = q.from(CategoryRelations.class);
        q.where(cb.equal(rootQuery.get(CategoryRelations_.entityId), entityId));
        em.createQuery(q).executeUpdate();
    }

    public boolean isCategoryUsedByAnyEntity(UUID categoryId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var rootQuery = q.from(CategoryRelations.class);
        q.select(cb.count(rootQuery));
        q.where(cb.equal(rootQuery.get(CategoryRelations_.category).get(Category_.id), categoryId));
        return em.createQuery(q).getSingleResult() > 0;
    }
}
