package com.provoly.ref.widget;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.datasource.DataSourceService;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.widget.dto.WidgetDto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class WidgetService {
    private ObjectMapper objectMapper;

    private UserService userService;

    private WidgetMapper mapper;

    private DataSourceService dataSourceService;

    private EntityIdService entityIdService;

    @PersistenceContext
    EntityManager em;

    public WidgetService(ObjectMapper objectMapper, UserService userService, WidgetMapper widgetMapper,
            DataSourceService dataSourceService, EntityIdService entityIdService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.mapper = widgetMapper;
        this.dataSourceService = dataSourceService;
        this.entityIdService = entityIdService;
    }

    //TODO: duplicate from NamedQueryService
    public Collection<ProvolyUserWidgetCatalog> getWidgetForCurrentUser() {
        var currentUser = userService.getCurrentUser();

        return getWidgetForUser(currentUser);
    }

    private Collection<ProvolyUserWidgetCatalog> getWidgetForUser(ProvolyUser user) {
        var cb = em.getCriteriaBuilder();
        var widgetQuery = cb.createQuery(WidgetCatalog.class);
        var root = widgetQuery.from(WidgetCatalog.class);

        widgetQuery.where(cb.or(
                isLevelPublic(cb, root),
                isBelongTo(cb, widgetQuery, root, user)));

        List<WidgetCatalog> widgetCatalogs = em.createQuery(widgetQuery).getResultList();
        return widgetCatalogs.stream().map(nq -> nq.getForUser(user)).toList();

    }

    /**
     * Build a predicate checking if NamedQuery is public
     */
    private Predicate isLevelPublic(CriteriaBuilder cb, Root<WidgetCatalog> namedQueryRoot) {
        return cb.equal(namedQueryRoot.get(WidgetCatalog_.visibilityType), VisibilityType.PUBLIC);
    }

    private Predicate isBelongTo(CriteriaBuilder cb, CriteriaQuery<WidgetCatalog> widget, Root<WidgetCatalog> root,
            ProvolyUser currentUser) {
        // Build a sub query to check if a ProvolyUserNamedQuery is associated to the NamedQuery
        var subUserNamedQuery = widget.subquery(Integer.class);
        var subUserNamedQueryRoot = subUserNamedQuery
                .correlate(root)
                .join(WidgetCatalog_.belongTo);

        var isCurrentUser = cb.equal(subUserNamedQueryRoot.get(ProvolyUserWidgetCatalog_.user), currentUser);

        var subquery = subUserNamedQuery
                .select(cb.literal(1))
                .where(isCurrentUser);
        return cb.exists(subquery);
    }

    @Transactional
    public void addWidget(WidgetDto dto) {

        verifyDatasourcesExist(dto); // throw error if false.

        try {
            // validate if content is a JSON
            objectMapper.readTree(dto.content);
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Json content is invalid");
        }

        WidgetCatalog widgetCatalog = findById(dto.id);
        var currentUser = userService.getCurrentUser();

        if (widgetCatalog == null) {
            widgetCatalog = mapper.toEntity(dto);
            widgetCatalog.setCreationDate(Instant.now());
            widgetCatalog.setModificationDate(Instant.now());
            entityIdService.saveEntity(widgetCatalog);
            widgetCatalog.add(currentUser, true);
        } else {
            // only creator can update widget
            checkCurrentUserOwnsWidget(widgetCatalog);
            mapper.update(dto, widgetCatalog);
        }
    }

    private void verifyDatasourcesExist(WidgetDto widgetDto) {
        if (widgetDto.datasource == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No datasource provided");
        }

        for (UUID thisDatasourceId : widgetDto.datasource) {
            dataSourceService.getDataSourceDetails(thisDatasourceId);
        }
    }

    @Transactional
    public ProvolyUserWidgetCatalog getMineById(UUID id) {
        var currentUser = userService.getCurrentUser();
        return entityIdService.getById(id, WidgetCatalog.class).getForUser(currentUser);
    }

    public WidgetCatalog getWidgetCatalogById(UUID id) {
        return entityIdService.getById(id, WidgetCatalog.class);
    }

    public Collection<WidgetCatalog> getAll() {
        return entityIdService.getAll(WidgetCatalog.class);
    }

    @Transactional
    public void delete(UUID id) {
        var widget = getWidgetCatalogById(id);
        checkCurrentUserOwnsWidget(widget);
        removeEntity(id);
    }

    public void removeWidgetCatalogIfExists(UUID id) {
        entityIdService.removeIfExists(id, WidgetCatalog.class);
    }

    public void removeEntity(UUID id) {
        entityIdService.removeEntity(id, WidgetCatalog.class);
    }

    private void checkCurrentUserOwnsWidget(WidgetCatalog widgetCatalog) {
        ProvolyUser user = userService.getCurrentUser();
        if (!widgetCatalog.isOwner(user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "This widget does not belong to user");
        }
    }

    public WidgetCatalog findById(UUID id) {
        return entityIdService.findById(id, WidgetCatalog.class);
    }

}