package com.provoly.ref.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import com.provoly.common.error.BusinessException;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.DashboardRepository;
import com.provoly.ref.dashboard.DashboardService;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.groups.*;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GrantServiceUTest {

    GrantService grantService;
    UserService userService;
    EntityManager em;
    DashboardService dashboardService;
    DashboardRepository dashboardRepository;
    GroupRepository groupRepository;
    DatasetRepository datasetRepository;
    Logger logger = Logger.getLogger(DatasetVersionService.class);

    @BeforeEach
    public void init() {
        userService = mock(UserService.class);
        dashboardService = mock(DashboardService.class);
        dashboardRepository = mock(DashboardRepository.class);
        groupRepository = mock(GroupRepository.class);
        datasetRepository = mock(DatasetRepository.class);
        grantService = new GrantService(userService, em, logger, dashboardRepository, groupRepository,
                datasetRepository);
    }

    @Test
    void can_write_same_user_isOk() {
        ProvolyUser provolyUser = new ProvolyUser();

        when(userService.getCurrentUser()).thenReturn(provolyUser);

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "", "");
        dashboard.setUser(provolyUser);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD));
    }

    @Test
    void can_see_same_user_isOk() {
        ProvolyUser provolyUser = new ProvolyUser();

        when(userService.getCurrentUser()).thenReturn(provolyUser);

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "", "");
        dashboard.setUser(provolyUser);

        Assertions.assertDoesNotThrow(() -> grantService.canSee(dashboard, WithGroupEntityType.DASHBOARD, new ProvolyUser()));
    }

    @Test
    void getAllUserAllowed_Dashboard_asAdmin_return_all() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dashboard firstDashboard = new Dashboard(UUID.randomUUID(), "", "");
        Dashboard secondDashboard = new Dashboard(UUID.randomUUID(), "", "");

        when(dashboardRepository.getAll()).thenReturn(List.of(firstDashboard, secondDashboard));
        when(dashboardRepository.getUserVisibleDashboards(user)).thenReturn(List.of(firstDashboard));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DASHBOARD, user);

        verify(dashboardRepository, times(1)).getAll();
        assertEquals(List.of(firstDashboard, secondDashboard), result);
    }

    @Test
    void getAllUserAllowed_Dashboard_withoutAdmin_return_allowed() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());
        user.setRoles(Set.of());

        Dashboard firstDashboard = new Dashboard(UUID.randomUUID(), "", "");
        Dashboard secondDashboard = new Dashboard(UUID.randomUUID(), "", "");

        when(dashboardRepository.getAll()).thenReturn(List.of(firstDashboard, secondDashboard));
        when(dashboardRepository.getUserVisibleDashboards(any())).thenReturn(List.of(firstDashboard));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DASHBOARD, new ProvolyUser());
        verify(dashboardRepository, times(1)).getUserVisibleDashboards(any());

        assertEquals(List.of(firstDashboard), result);
    }

    @Test
    void getAllUserAllowed_Dataset_asAdmin_return_All() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dataset firstDataset = new Dataset(UUID.randomUUID());
        Dataset secondDataset = new Dataset(UUID.randomUUID());

        when(datasetRepository.getAll()).thenReturn(List.of(firstDataset, secondDataset));
        when(datasetRepository.getAllowedDataset(any())).thenReturn(List.of(firstDataset));
        when(userService.getCurrentUser()).thenReturn(user);

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DATASET, user);
        verify(datasetRepository, times(1)).getAll();

        assertEquals(List.of(firstDataset), result);
    }

    @Test
    void canWrite_Dataset_asOwner_isOk() {
        ProvolyUser user = new ProvolyUser();

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(user);

        when(userService.getCurrentUser()).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dataset, WithGroupEntityType.DATASET));
    }

    @Test
    void canWrite_Dataset_notOwner_isKo() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);

        var error = Assertions.assertThrows(BusinessException.class,
                () -> grantService.canWrite(dataset, WithGroupEntityType.DATASET));
    }

    @Test
    void canWrite_Dataset_notOwner_roleAdmin_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dataset, WithGroupEntityType.DATASET));
    }

    @Test
    void canWrite_Dashboard_notOwner_roleAdmin_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "Name", "Information");
        dashboard.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD));
    }

    @Test
    void canWrite_Dashboard_notOwner_notInWriteGroup_isKo() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());
        var group = new Group();
        group.setName("my_group");

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "Name", "Information");
        dashboard.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);
        var groupRelation = new DashboardGroupRelations(UUID.randomUUID(), group, dashboard.getId(), true);
        when(groupRepository.getGroupsByEntityId(dashboard.getId())).thenReturn(List.of(groupRelation));

        Assertions.assertThrows(BusinessException.class, () -> grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD));
    }

    @Test
    void canWrite_Dashboard_notOwner_inWriteGroup_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());
        ;
        var group = new Group();
        group.setName("my_group");
        user.setGroups(List.of(group));

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "Name", "Information");
        dashboard.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);
        var groupRelation = new DashboardGroupRelations(UUID.randomUUID(), group, dashboard.getId(), true);
        when(groupRepository.getGroupsByEntityId(dashboard.getId())).thenReturn(List.of(groupRelation));

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, WithGroupEntityType.DASHBOARD));
    }
}
