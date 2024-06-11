package com.provoly.ref.widget;

import static com.provoly.ref.groups.WithGroupEntityType.WIDGET;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.ref.datasource.DataSourceService;
import com.provoly.ref.groups.GrantService;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.groups.WithGroupEntityType;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.widget.dto.WidgetWriteDto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class WidgetService {
    private ObjectMapper objectMapper;
    private UserService userService;
    private WidgetMapper mapper;
    private DataSourceService dataSourceService;
    private GroupService groupService;
    private GrantService grantService;
    private WidgetRepository widgetRepository;

    public WidgetService(ObjectMapper objectMapper, UserService userService, WidgetMapper widgetMapper,
            DataSourceService dataSourceService, GroupService groupService,
            GrantService grantService, WidgetRepository widgetRepository) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.mapper = widgetMapper;
        this.dataSourceService = dataSourceService;
        this.groupService = groupService;
        this.grantService = grantService;
        this.widgetRepository = widgetRepository;
    }

    @Transactional
    public void addWidget(WidgetWriteDto dto) {
        verifyDatasourcesExist(dto); // throw error if false.

        try {
            // validate if content is a JSON
            objectMapper.readTree(dto.content());
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Json content is invalid");
        }

        WidgetCatalog widgetCatalog = widgetRepository.findById(dto.id());
        var currentUser = userService.getCurrentUser();

        if (widgetCatalog == null) {
            updateGroups(dto);
            createWidget(dto, currentUser);
        } else {
            // only creator can update widget
            grantService.canWrite(widgetCatalog, WIDGET, userService.getCurrentUser());
            updateGroups(dto);
            mapper.update(dto, widgetCatalog);
        }
    }

    @Transactional
    public WidgetCatalog getMineById(UUID id) {
        ProvolyUser currentUser = userService.getCurrentUser();
        return Optional.of(widgetRepository.getWidgetCatalogById(id))
                .filter(widget -> grantService.canSee(widget, WithGroupEntityType.WIDGET, currentUser))
                .orElseThrow(() -> new ProvolyNotFoundException("Widget : %s inexistant.".formatted(id)));
    }

    @Transactional
    public void delete(UUID id) {
        var widget = widgetRepository.getWidgetCatalogById(id);
        grantService.canWrite(widget, WIDGET, userService.getCurrentUser());
        widgetRepository.removeEntity(id);
    }

    private void createWidget(WidgetWriteDto dto, ProvolyUser currentUser) {
        WidgetCatalog widgetCatalog = mapper.toEntity(dto);
        widgetCatalog.setCreationDate(Instant.now());
        widgetCatalog.setModificationDate(Instant.now());
        widgetCatalog.setUser(currentUser);
        widgetRepository.saveWidget(widgetCatalog);
    }

    private void verifyDatasourcesExist(WidgetWriteDto widgetDto) {
        if (widgetDto.datasource() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No datasource provided");
        }

        for (UUID thisDatasourceId : widgetDto.datasource()) {
            dataSourceService.getDataSourceDetails(thisDatasourceId);
        }
    }

    public Collection<WidgetCatalog> getAllowedWidgets(ProvolyUser provolyUser) {
        return grantService.getAllUserAllowed(WIDGET, provolyUser);
    }

    private void updateGroups(WidgetWriteDto dto) {
        if (dto.groups() != null) {
            var rightsByGroup = dto.groups().stream()
                    .collect(Collectors.toMap(groupName -> groupName, _ignored -> List.of(
                            GroupRights.READ)));
            groupService.updateEntityGroups(rightsByGroup, dto.id(), WIDGET);
        }
    }
}