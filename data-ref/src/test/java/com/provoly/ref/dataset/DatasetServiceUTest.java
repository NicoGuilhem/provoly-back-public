package com.provoly.ref.dataset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.model.CategoryDto;
import com.provoly.ref.category.*;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.groups.GrantService;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.model.AssociationService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatasetServiceUTest {

    DatasetService datasetService;
    DatasetVersionService datasetVersionService;
    AssociationService associationService;
    DatasetVersionRepository versionRepository;
    GroupService groupService;
    GroupRepository groupRepository;
    DatasetMapper datasetMapper;
    UserService userService;
    GrantService grantService;
    DatasetRepository datasetRepository;
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    CategoryService categoryService;

    @BeforeEach
    public void init() {
        datasetVersionService = mock(DatasetVersionService.class);
        associationService = mock(AssociationService.class);
        versionRepository = mock(DatasetVersionRepository.class);
        groupService = mock(GroupService.class);
        groupRepository = mock(GroupRepository.class);
        datasetMapper = mock(DatasetMapper.class);
        userService = mock(UserService.class);
        datasetRepository = mock(DatasetRepository.class);
        datasetMapper = mock(DatasetMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        categoryService = mock(CategoryService.class);
        categoryRepository = mock(CategoryRepository.class);

        datasetService = new DatasetService(datasetVersionService, associationService, versionRepository,
                groupService, groupRepository, datasetMapper, userService,
                grantService, datasetRepository, categoryService);
    }

    @Test
    void test_save_withoutGroups_shouldSucceed_and_not_update() {
        var datasetDto = new DatasetDto(UUID.randomUUID(), "Nom", UUID.randomUUID(), DatasetType.CLOSED);
        var dataset = new Dataset(datasetDto.getId());
        var user = new ProvolyUser();

        when(datasetMapper.toModel(datasetDto)).thenReturn(dataset);
        when(userService.getCurrentUser()).thenReturn(user);
        when(datasetRepository.exists(dataset)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> datasetService.save(datasetDto));
        verify(groupService, times(0)).updateEntityGroups(anyMap(), any(), any());
    }

    @Test
    void test_save_withTag_shouldSucceed() {
        CategoryDto categoryDto = new CategoryDto(UUID.randomUUID(), "Tag name");
        var datasetDto = new DatasetDto(UUID.randomUUID(), "Nom", UUID.randomUUID(), DatasetType.CLOSED, List.of(),
                List.of(categoryDto.id()));
        var dataset = new Dataset(datasetDto.getId());
        var user = new ProvolyUser();

        when(datasetMapper.toModel(datasetDto)).thenReturn(dataset);
        when(userService.getCurrentUser()).thenReturn(user);
        when(datasetRepository.exists(dataset)).thenReturn(true);
        when(categoryRepository.getById(any()))
                .thenReturn(new Category(categoryDto.id(), categoryDto.name(), WithCategoryEntityType.DATASET));

        Assertions.assertDoesNotThrow(() -> datasetService.save(datasetDto));
        verify(categoryService, times(1)).updateEntityCategories(anyList(), any(), any());
    }

    @Test
    void test_save_withTagNull_shouldModifyTags() {
        CategoryDto categoryDto = new CategoryDto(UUID.randomUUID(), "Tag name");
        var datasetDto = new DatasetDto(UUID.randomUUID(), "Nom", UUID.randomUUID(), DatasetType.CLOSED, List.of(), null);
        var dataset = new Dataset(datasetDto.getId());
        var user = new ProvolyUser();

        when(datasetMapper.toModel(datasetDto)).thenReturn(dataset);
        when(userService.getCurrentUser()).thenReturn(user);
        when(datasetRepository.exists(dataset)).thenReturn(true);
        when(categoryRepository.getById(any()))
                .thenReturn(new Category(categoryDto.id(), categoryDto.name(), WithCategoryEntityType.DATASET));

        Assertions.assertDoesNotThrow(() -> datasetService.save(datasetDto));
        verify(categoryRepository, times(0)).deleteCategoriesRelationsFromEntity(any());
        verify(categoryRepository, times(0)).saveCategoryRelations(any(), any());
    }
}
