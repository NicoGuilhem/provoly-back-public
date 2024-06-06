package com.provoly.category;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.provoly.ref.category.Category;
import com.provoly.ref.category.CategoryRepository;
import com.provoly.ref.category.CategoryService;
import com.provoly.ref.category.WithCategoryEntityType;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CategoryServiceUnitTest {
    CategoryService categoryService;
    Logger log;
    CategoryRepository categoryRepository;

    @BeforeEach
    public void init() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryService(log, categoryRepository);
    }

    @Test
    void test_save_with_two_category_shouldThrow() {
        assertThatThrownBy(() -> categoryService.updateEntityCategories(List.of(UUID.randomUUID(), UUID.randomUUID()),
                UUID.randomUUID(), WithCategoryEntityType.ATTRIBUTES))
                .hasMessage("You are not allowed to set more than one category to attribute");
    }

    @Test
    void test_update_category_with_different_entity_type_shouldThrow() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.getById(any()))
                .thenReturn(new Category(UUID.randomUUID(), "category_def", WithCategoryEntityType.DATASET));

        assertThatThrownBy(
                () -> categoryService.updateEntityCategories(List.of(categoryId), UUID.randomUUID(),
                        WithCategoryEntityType.ATTRIBUTES))
                .hasMessage("Unrecognized categories: %s".formatted(List.of(categoryId)));
    }

}
