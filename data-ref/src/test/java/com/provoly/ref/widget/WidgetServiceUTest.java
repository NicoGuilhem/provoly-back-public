package com.provoly.ref.widget;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.ref.datasource.DataSourceService;
import com.provoly.ref.groups.GrantService;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.widget.dto.WidgetWriteDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WidgetServiceUTest {

    WidgetService widgetService;
    ObjectMapper objectMapper;
    UserService userService;
    WidgetMapper widgetMapper;
    DataSourceService dataSourceService;
    GrantService grantService;
    WidgetRepository widgetRepository;
    GroupService groupService;

    @BeforeEach
    public void init() {
        objectMapper = mock(ObjectMapper.class);
        userService = mock(UserService.class);
        dataSourceService = mock(DataSourceService.class);
        grantService = mock(GrantService.class);
        widgetRepository = mock(WidgetRepository.class);
        widgetMapper = mock(WidgetMapper.class);
        groupService = mock(GroupService.class);
        widgetService = new WidgetService(objectMapper, userService, widgetMapper, dataSourceService, groupService,
                grantService, widgetRepository);
    }

    @Test
    void add_widget_contentInvalid_isKo() throws JsonProcessingException {
        ProvolyUser provolyUser = new ProvolyUser();
        WidgetWriteDto widget = new WidgetWriteDto(UUID.randomUUID(), "widgetName", "", "", "{", List.of(), false, List.of());

        when(userService.getCurrentUser()).thenReturn(provolyUser);
        when(objectMapper.readTree(anyString())).thenThrow(JsonProcessingException.class);

        var error = assertThrows(BusinessException.class, () -> widgetService.addWidget(widget));
    }

    @Test
    void add_widget_with_same_name_isKo() throws JsonProcessingException {
        ProvolyUser provolyUser = new ProvolyUser();
        WidgetWriteDto widget = new WidgetWriteDto(UUID.randomUUID(), "widgetName", "", "", "{", List.of(), false, List.of());

        when(userService.getCurrentUser()).thenReturn(provolyUser);
        when(objectMapper.readTree(anyString())).thenThrow(JsonProcessingException.class);

        var error = assertThrows(BusinessException.class, () -> widgetService.addWidget(widget));
    }
}
