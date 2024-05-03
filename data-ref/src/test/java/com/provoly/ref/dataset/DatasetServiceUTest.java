package com.provoly.ref.dataset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.groups.GrantService;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.model.AssociationService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatasetServiceUTest {

    static DatasetService datasetService;
    static DatasetVersionService datasetVersionService;
    static AssociationService associationService;
    static DatasetVersionRepository versionRepository;
    static GroupService groupService;
    static GroupRepository groupRepository;
    static DatasetMapper datasetMapper;
    static Logger logger = Logger.getLogger(DatasetVersionService.class);
    static UserService userService;
    static GrantService grantService;
    static DatasetRepository datasetRepository;

    @BeforeAll
    static void before() {
        datasetVersionService = mock(DatasetVersionService.class);
        associationService = mock(AssociationService.class);
        versionRepository = mock(DatasetVersionRepository.class);
        groupService = mock(GroupService.class);
        groupRepository = mock(GroupRepository.class);
        datasetMapper = mock(DatasetMapper.class);
        userService = mock(UserService.class);
        datasetRepository = mock(DatasetRepository.class);
        datasetMapper = mock(DatasetMapper.class);

        datasetService = new DatasetService(datasetVersionService, associationService, versionRepository,
                groupService, groupRepository, datasetMapper, logger, userService, grantService, datasetRepository);
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
}
