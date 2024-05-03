package com.provoly.ref.group;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.user.SystemGroup;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.groups.*;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GroupServiceUTest {

    static Logger logger = Logger.getLogger(DatasetVersionService.class);
    static GroupService groupService;
    static GroupMapper groupMapper;
    static EntityManager entityManager;
    static GroupRepository groupRepository;

    @BeforeAll
    static void before() {
        groupMapper = mock(GroupMapper.class);
        entityManager = mock(EntityManager.class);
        groupRepository = mock(GroupRepository.class);
        groupService = new GroupService(groupMapper, logger, groupRepository);
    }

    @Test
    void adding_systemGroup_with_write_shouldThrow() {
        Map<String, List<GroupRights>> rightsByGroup = new HashMap<String, List<GroupRights>>();
        rightsByGroup.put(SystemGroup.ALL.toString(), List.of(GroupRights.WRITE));
        when(groupRepository.getGroupByName("ALL")).thenReturn(new Group(UUID.randomUUID(), SystemGroup.ALL.name(), true));

        Assertions.assertThrows(BusinessException.class,
                () -> groupService.associateGroupToEntity(rightsByGroup, UUID.randomUUID(), WithGroupEntityType.DASHBOARD));
    }
}
