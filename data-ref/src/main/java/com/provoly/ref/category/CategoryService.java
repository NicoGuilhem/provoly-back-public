package com.provoly.ref.category;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import org.jboss.logging.Logger;

@ApplicationScoped
public class CategoryService {

    private Logger log;
    private CategoryRepository categoryRepository;

    public CategoryService(Logger log, CategoryRepository categoryRepository) {
        this.log = log;
        this.categoryRepository = categoryRepository;
    }

    public void updateEntityCategories(List<UUID> categoryIds, UUID entityId, WithCategoryEntityType entityType) {
        if (categoryIds == null) {
            log.debugf("No category provided for %s entity %s", entityType.name(), entityId);
            return;
        }
        checkIfCategoryIsUpdatable(categoryIds, entityType);
        List<UUID> categoryRelationsToDelete = categoryRepository.getCategoriesByEntityId(entityId).stream()
                .map(CategoryRelations::getId).toList();
        log.debugf("Remove categories relations %s", categoryRelationsToDelete);
        categoryRepository.deleteCategoriesRelationsFromEntity(categoryRelationsToDelete);
        saveCategoryRelations(categoryIds, entityId, entityType);
    }

    public void save(Category category) {
        if (getCategoryById(category.getId()) != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "You're not allowed to modify the category");
        }
        categoryRepository.checkNameAlreadyExists(category);
        categoryRepository.save(category);
    }

    public List<Category> getAll(WithCategoryEntityType withCategoryEntityType) {
        return categoryRepository.getAll(withCategoryEntityType);
    }

    public void deleteAllByEntityId(UUID id) {
        categoryRepository.deleteAllByEntityId(id);
    }

    public boolean isCategoryUsedByAnyEntity(UUID categoryId) {
        return categoryRepository.isCategoryUsedByAnyEntity(categoryId);
    }

    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id);
    }

    private void checkIfCategoryIsUpdatable(List<UUID> categoryIds, WithCategoryEntityType entityType) {
        if (entityType.equals(WithCategoryEntityType.ATTRIBUTES) && categoryIds.size() > 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You are not allowed to set more than one category to attribute");
        }
        var inexistantCategories = categoryIds.stream()
                .filter(categoryId -> categoryRepository.getById(categoryId).getWithCategoryEntityType() != entityType)
                .toList();
        if (!inexistantCategories.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unrecognized categories: %s".formatted(inexistantCategories));
        }
    }

    private void saveCategoryRelations(List<UUID> categoryIds, UUID entityId, WithCategoryEntityType entityType) {
        categoryIds.forEach(categoryId -> {
            Category category = categoryRepository.getById(categoryId);
            log.debugf("Associate category %s to %s %s", category.getName(), entityType, entityId);
            categoryRepository.saveCategoryRelations(category, entityId);
        });
    }
}
