package com.provoly.ref.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.user.Role;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.DashboardService;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.groups.*;
import com.provoly.ref.model.OClass;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.widget.WidgetCatalog;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GrantServiceUTest {

    GrantService grantService;
    UserService userService;
    DashboardService dashboardService;
    EntityIdRepository entityIdRepository;
    GroupRepository groupRepository;
    DatasetRepository datasetRepository;
    WidgetCatalog widgetCatalog;
    DatasetVersionRepository datasetVersionRepository;
    Logger logger = Logger.getLogger(DatasetVersionService.class);

    @BeforeEach
    public void init() {
        userService = mock(UserService.class);
        dashboardService = mock(DashboardService.class);
        entityIdRepository = mock(EntityIdRepository.class);
        groupRepository = mock(GroupRepository.class);
        datasetRepository = mock(DatasetRepository.class);
        datasetVersionRepository = mock(DatasetVersionRepository.class);
        widgetCatalog = mock(WidgetCatalog.class);
        grantService = new GrantService(logger, entityIdRepository, groupRepository,
                datasetRepository, datasetVersionRepository);
    }

    @Test
    void can_write_same_user_isOk() {
        ProvolyUser provolyUser = new ProvolyUser();

        when(userService.getCurrentUser()).thenReturn(provolyUser);

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "", "");
        dashboard.setUser(provolyUser);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, provolyUser));
    }

    @Test
    void can_see_same_user_isOk() {
        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail", List.of());
        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "", "");
        dashboard.setUser(owner);

        Assertions.assertTrue(grantService.canSee(dashboard, owner));
    }

    @Test
    void can_see_admin_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail", List.of("administrate"));

        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "lasr", "mail", List.of());
        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "", "");
        dashboard.setUser(owner);

        Assertions.assertTrue(grantService.canSee(dashboard, user));
    }

    @Test
    void can_see_different_user_with_group_isOk() {
        Group group = new Group(UUID.randomUUID(), "group", false);
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "subject", "name", "lasr", "mail", List.of());
        user.setGroups(List.of(group));

        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail", List.of());
        UUID dashboardId = UUID.randomUUID();
        Dashboard dashboard = new Dashboard(dashboardId, "", "");
        dashboard.setUser(owner);

        when(groupRepository.getGroupsByEntityId(dashboardId))
                .thenReturn(List.of(new DashboardGroupRelations(UUID.randomUUID(), group, dashboard.getId(), false)));

        Assertions.assertTrue(grantService.canSee(dashboard, user));
    }

    @Test
    void can_see_different_user_without_group_isKo() {
        Group group = new Group(UUID.randomUUID(), "group", false);
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "subject", "name", "lasr", "mail", List.of());

        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail", List.of());
        UUID dashboardId = UUID.randomUUID();
        Dashboard dashboard = new Dashboard(dashboardId, "", "");
        dashboard.setUser(owner);

        when(groupRepository.getGroupsByEntityId(dashboardId))
                .thenReturn(List.of(new DashboardGroupRelations(UUID.randomUUID(), group, dashboard.getId(), false)));

        Assertions.assertFalse(grantService.canSee(dashboard, user));
    }

    @Test
    void getAllUserAllowed_Dashboard_asAdmin_return_all() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dashboard firstDashboard = new Dashboard(UUID.randomUUID(), "", "");
        Dashboard secondDashboard = new Dashboard(UUID.randomUUID(), "", "");

        when(entityIdRepository.getAll(Dashboard.class)).thenReturn(List.of(firstDashboard, secondDashboard));
        when(groupRepository.getAllowedEntityId(user, Dashboard.class)).thenReturn(List.of(firstDashboard));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DASHBOARD, user);

        verify(entityIdRepository, times(1)).getAll(Dashboard.class);
        assertEquals(List.of(firstDashboard, secondDashboard), result);
    }

    @Test
    void getAllUserAllowed_Dashboard_withoutAdmin_return_allowed() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());
        user.setRoles(Set.of());

        Dashboard firstDashboard = new Dashboard(UUID.randomUUID(), "", "");
        Dashboard secondDashboard = new Dashboard(UUID.randomUUID(), "", "");

        when(entityIdRepository.getAll(Dashboard.class)).thenReturn(List.of(firstDashboard, secondDashboard));
        when(groupRepository.getAllowedEntityId(any(), any())).thenReturn(List.of(firstDashboard));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DASHBOARD, new ProvolyUser());
        verify(groupRepository, times(1)).getAllowedEntityId(any(), any());

        assertEquals(List.of(firstDashboard), result);
    }

    @Test
    void getAllUserAllowed_Dataset_asAdmin_return_All() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dataset firstDataset = new Dataset(UUID.randomUUID());
        Dataset secondDataset = new Dataset(UUID.randomUUID());

        when(entityIdRepository.getAll(Dataset.class)).thenReturn(List.of(firstDataset, secondDataset));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.DATASET, user);

        verify(entityIdRepository, times(1)).getAll(Dataset.class);
        verify(groupRepository, never()).getAllowedEntityId(any(), any());

        assertEquals(List.of(firstDataset, secondDataset), result);
    }

    @Test
    void canWrite_Dataset_asOwner_isOk() {
        ProvolyUser user = new ProvolyUser();

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(user);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dataset, user));
    }

    @Test
    void canWrite_Dataset_notOwner_isKo() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(new ProvolyUser());

        Assertions.assertThrows(BusinessException.class,
                () -> grantService.canWrite(dataset, user));
    }

    @Test
    void canWrite_Dataset_notOwner_roleAdmin_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dataset dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(new ProvolyUser());

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dataset, user));
    }

    @Test
    void canWrite_Dashboard_notOwner_roleAdmin_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "Name", "Information");
        dashboard.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, user));
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

        Assertions.assertThrows(BusinessException.class,
                () -> grantService.canWrite(dashboard, user));
    }

    @Test
    void canWrite_Dashboard_notOwner_inWriteGroup_isOk() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());

        var group = new Group();
        group.setName("my_group");
        user.setGroups(List.of(group));

        Dashboard dashboard = new Dashboard(UUID.randomUUID(), "Name", "Information");
        dashboard.setUser(new ProvolyUser());

        when(userService.getCurrentUser()).thenReturn(user);
        var groupRelation = new DashboardGroupRelations(UUID.randomUUID(), group, dashboard.getId(), true);
        when(groupRepository.getGroupsByEntityId(dashboard.getId())).thenReturn(List.of(groupRelation));

        Assertions.assertDoesNotThrow(() -> grantService.canWrite(dashboard, user));
    }

    @Test
    void can_see_widget_same_user_isOk() {
        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail", List.of());
        WidgetCatalog widget = new WidgetCatalog(UUID.randomUUID());
        widget.setUser(owner);

        Assertions.assertTrue(grantService.canSee(widget, owner));
    }

    @Test
    void getAllUserAllowed_Widget_asAdmin_return_all() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of(Role.STR_ADMINISTRATE));

        WidgetCatalog firstWidgetCatalog = new WidgetCatalog(UUID.randomUUID());
        WidgetCatalog secondWidgetCatalog = new WidgetCatalog(UUID.randomUUID());

        when(entityIdRepository.getAll(WidgetCatalog.class)).thenReturn(List.of(firstWidgetCatalog, secondWidgetCatalog));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.WIDGET, user);

        assertEquals(List.of(firstWidgetCatalog, secondWidgetCatalog), result);
        verify(groupRepository, never()).getAllowedEntityId(any(), any());
    }

    @Test
    void getAllUserAllowed_Widget_withoutAdmin_return_allowed() {
        ProvolyUser user = new ProvolyUser(UUID.randomUUID(), "sub", "name", "last", "email", List.of());
        user.setRoles(Set.of());

        WidgetCatalog firstWidgetCatalog = new WidgetCatalog(UUID.randomUUID());

        when(groupRepository.getAllowedEntityId(user, WidgetCatalog.class)).thenReturn(List.of(firstWidgetCatalog));

        var result = grantService.getAllUserAllowed(WithGroupEntityType.WIDGET, user);

        verify(groupRepository, times(1)).getAllowedEntityId(any(), any());
        assertEquals(List.of(firstWidgetCatalog), result);
    }

    @Test
    void getAllUserAllowed_DatasetByClass_withoutAdmin_return_allowed() {
        OClass oclass = new OClass(UUID.randomUUID());
        var firstDataset = createDataset(oclass);
        var secondDataset = createDataset(oclass);
        ProvolyUser owner = new ProvolyUser(UUID.randomUUID(), "subject", "name", "last", "mail",
                List.of(Role.STR_ADMINISTRATE));

        when(datasetRepository.getAllForClass(any())).thenReturn(List.of(firstDataset, secondDataset));

        var result = grantService.getUserAllowedDatasetsByClass(owner, oclass.getId());
        verify(datasetRepository, times(1)).getAllForClass(any());

        assertEquals(List.of(firstDataset, secondDataset), result);
    }

    private Dataset createDataset(OClass oclass) {
        var dataset = new Dataset(UUID.randomUUID());
        dataset.setUser(new ProvolyUser());
        dataset.setoClass(oclass);
        dataset.setName("Nom");
        dataset.setType(DatasetType.CLOSED);
        return dataset;
    }
}
