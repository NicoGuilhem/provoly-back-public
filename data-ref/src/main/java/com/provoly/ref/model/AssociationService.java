package com.provoly.ref.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.ref.abac.AbacService;
import com.provoly.ref.dashboard.DashboardService;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.link.Link;
import com.provoly.ref.link.Link_;
import com.provoly.ref.model.field.Field_;
import com.provoly.ref.searchrequest.*;
import com.provoly.ref.user.NamedQuery;
import com.provoly.ref.user.NamedQueryService;
import com.provoly.ref.user.NamedQuery_;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.widget.WidgetService;

import org.jboss.logging.Logger;

@ApplicationScoped
public class AssociationService {

    private EntityManager em;
    private AbacService abacService;
    private EntityIdRepository entityIdRepository;
    private NamedQueryService namedQueryService;
    private DashboardService dashboardService;
    private WidgetService widgetService;
    private Logger logger;

    public AssociationService(EntityManager em, AbacService abacService,
            EntityIdRepository entityIdRepository, NamedQueryService namedQueryService,
            DashboardService dashboardService, WidgetService widgetService,
            Logger logger) {
        this.em = em;
        this.abacService = abacService;
        this.entityIdRepository = entityIdRepository;
        this.namedQueryService = namedQueryService;
        this.dashboardService = dashboardService;
        this.widgetService = widgetService;
        this.logger = logger;
    }

    @Transactional
    public List<AssociationDto> getAbacAssociations(UUID id) {
        Query q = em.createNativeQuery("""
                    %s
                        SELECT distinct CAST(abac.id as TEXT), abac.name, 'PUBLIC' as visibility_type, 'ABAC' as type
                        FROM abac_rule abac
                        INNER JOIN usage_attr ua on ua.composed_condition_id = abac.condition_id
                        UNION ALL
                        SELECT distinct CAST(abac.id as TEXT), abac.name, 'PUBLIC' as visibility_type, 'ABAC' as type
                        FROM abac_rule abac
                        INNER JOIN condition con_sim on con_sim.id = abac.condition_id
                        where con_sim.attribute_id = :id
                """.formatted(RECURSIVE_CONDITION_QUERY), Tuple.class);
        var result = q.setParameter("id", id).getResultList();
        return mapSqlIntoAssociations(result);
    }

    public AssociationsDto getClassAssociations(UUID oClassId) {
        List<AssociationDto> result = new ArrayList<>();
        result.addAll(getDatasetAssociationsForClass(oClassId));
        result.addAll(getAbacRulesAssociationsForClass(oClassId));
        var oclass = entityIdRepository.getById(oClassId, OClass.class);
        result.addAll(getLinkAssociations(oclass.getId()));
        result.addAll(getNamedQueryAssociationsForClass(oclass.getId()));
        return mapAssociationDtoInAssociationsDto(result);
    }

    @Transactional
    public AssociationsDto getFieldAssociations(UUID id) {
        List<AssociationDto> associations = new ArrayList<>();
        associations.addAll(getOClassAssociationsForField(id));
        associations.addAll(getNamedQueryAssociationForField(id));
        return mapAssociationDtoInAssociationsDto(associations);
    }

    @Transactional
    public AssociationsDto getAttributeAssociations(UUID attributeId) {
        var oclass = entityIdRepository.getById(attributeId, AttributeDef.class).getOclass();
        List<AssociationDto> result = new ArrayList<>();
        result.addAll(getNamedQueryAssociations(attributeId));
        result.addAll(getLinkAssociations(oclass.getId()));
        result.addAll(getAbacAssociations(attributeId));
        return mapAssociationDtoInAssociationsDto(result);
    }

    public AssociationsDto mapAssociationDtoInAssociationsDto(List<AssociationDto> associationDtos) {
        List<AssociationDto> associations = associationDtos.stream()
                .filter(this::canCurrentUserSee)
                .toList();
        return new AssociationsDto(associations, associations.size() != associationDtos.size());
    }

    private static final String RECURSIVE_CONDITION_QUERY = """
            WITH RECURSIVE usage_attr AS (
                SELECT composed_id, composed_condition_id, 1 as LEVEL
                FROM condition_condition c
                WHERE composed_id in (
                    SELECT id
                    FROM condition
                    where attribute_id = :id
                    )
                UNION ALL
                SELECT con_con.composed_id, con_con.composed_condition_id, usage_attr.level + 1
                FROM usage_attr
                JOIN condition_condition con_con on con_con.composed_id = usage_attr.composed_condition_id )
            """;

    private Collection<AssociationDto> getOClassAssociationsForField(UUID uuid) {
        Collection<AssociationDto> associationDtos = new ArrayList<>();
        logger.debugf("Searching associated OClass for field: %s", uuid);
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<AttributeDef> criteriaQuery = cb.createQuery(AttributeDef.class);
        Root<AttributeDef> root = criteriaQuery.from(AttributeDef.class);

        criteriaQuery.where(
                cb.equal(root.get(AttributeDef_.field).get(Field_.id), uuid)).distinct(true);

        Collection<AttributeDef> results = em.createQuery(criteriaQuery).getResultList();
        logger.debugf("Result list of attibute_def as size %d", results.size());

        associationDtos.addAll(
                results.stream().map(
                        attributeDef -> new AssociationDto(
                                attributeDef.getOclass().getId(),
                                attributeDef.getOclass().getName(),
                                VisibilityType.PUBLIC,
                                AssociationsType.OCLASS))
                        .toList());

        return associationDtos;
    }

    private Collection<AssociationDto> getNamedQueryAssociationForField(UUID uuid) {
        logger.debugf("Searching associated Named Queries for field: %s", uuid);
        Collection<AssociationDto> associationDtos = new ArrayList<>();
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<NamedQuery> query = cb.createQuery(NamedQuery.class);
        // creating subquery to filter field.id in field_condition
        Subquery<UUID> fieldConditonSubquery = query.subquery(UUID.class);
        Root<FieldCondition> fieldConditions = fieldConditonSubquery.from(FieldCondition.class);
        fieldConditonSubquery.where(
                cb.equal(fieldConditions.get(FieldCondition_.field).get(Field_.id), uuid));
        // Select search_request.id where field_condition.field_id matching
        fieldConditonSubquery.select(fieldConditions.get(FieldCondition_.searchRequest).get(SearchRequest_.id));
        // Join named_query and search_request
        Join<NamedQuery, SearchRequest> namedQuerySearchRequests = query.from(NamedQuery.class).join(NamedQuery_.request);
        // get named_quey where named_query.request_id are matching with field_condition.id filtered list
        query.where(
                cb.in(namedQuerySearchRequests.get(SearchRequest_.id)).value(fieldConditonSubquery));

        Collection<NamedQuery> associateNamequeries = em.createQuery(query).getResultList();

        logger.debugf("Result list of name queries association(s) to field %s", uuid);

        associationDtos.addAll(associateNamequeries.stream().map(
                namedQuery -> new AssociationDto(
                        namedQuery.getId(),
                        namedQuery.getName(),
                        namedQuery.getVisibilityType(),
                        AssociationsType.NAMED_QUERY))
                .toList());

        return associationDtos;
    }

    private List<AssociationDto> getNamedQueryAssociations(UUID id) {
        Query q = em.createNativeQuery("""
                    %s
                    SELECT distinct CAST(nq.id as TEXT), nq.name, nq.visibility_type, 'NAMED_QUERY' as type
                   FROM named_query nq
                   INNER JOIN mono_class_search_request request on request.id = nq.request_id
                   INNER JOIN usage_attr ua on ua.composed_condition_id = request.condition_id
                   UNION ALL
                   SELECT distinct CAST(nq.id as TEXT), nq.name, nq.visibility_type, 'NAMED_QUERY' as type
                   FROM named_query nq
                   INNER JOIN mono_class_search_request request on request.id = nq.request_id
                   INNER JOIN condition con_sim on con_sim.id = request.condition_id
                   where con_sim.attribute_id = :id
                """.formatted(RECURSIVE_CONDITION_QUERY), Tuple.class);
        List<Tuple> result = q.setParameter("id", id).getResultList();
        return mapSqlIntoAssociations(result);
    }

    private List<AssociationDto> getLinkAssociations(UUID oclassId) {
        List<AssociationDto> associationDtos = new ArrayList<>();
        var cb = em.getCriteriaBuilder();
        var linkQuery = cb.createQuery(Link.class);
        var root = linkQuery.from(Link.class);

        linkQuery.where(cb.or(
                cb.equal(root.get(Link_.attributeSource).get(AttributeDef_.oclass).get(OClass_.id), oclassId),
                cb.equal(root.get(Link_.attributeDestination).get(AttributeDef_.oclass).get(OClass_.id), oclassId)));
        var result = em.createQuery(linkQuery).getResultList();
        result.forEach(link -> associationDtos
                .add(new AssociationDto(link.getId(), String.valueOf(AssociationsType.LINK), VisibilityType.PUBLIC,
                        AssociationsType.LINK)));
        return associationDtos;
    }

    private List<AssociationDto> getDatasetAssociationsForClass(UUID id) {
        var cb = em.getCriteriaBuilder();
        var datasetQuery = cb.createQuery(Dataset.class);
        var root = datasetQuery.from(Dataset.class);

        datasetQuery.where(cb.equal(root.get(Dataset_.oClass).get(OClass_.id), id));
        return em.createQuery(datasetQuery).getResultList()
                .stream()
                .map(dataset -> new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.DATASET))
                .toList();
    }

    private List<AssociationDto> getAbacRulesAssociationsForClass(UUID id) {
        List<AssociationDto> associationDtos = new ArrayList<>();
        var abac = abacService.getAllForClass(id, true);
        abac.forEach(dataset -> associationDtos
                .add(new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.ABAC)));
        return associationDtos;
    }

    private boolean canCurrentUserSee(AssociationDto associationDto) {
        boolean isOwnByUser = false;
        try {
            switch (associationDto.getType()) {
                case DASHBOARD ->
                    isOwnByUser = dashboardService.getDashboardById(associationDto.getId()) != null;
                case WIDGET -> isOwnByUser = widgetService.getMineById(associationDto.getId()) != null;
                case NAMED_QUERY -> isOwnByUser = namedQueryService.getMineById(associationDto.getId()) != null;
                case OCLASS, ABAC, LINK -> isOwnByUser = true;
                default -> throw new IllegalStateException("Unexpected value: " + associationDto.getType());
            }
        } catch (BusinessException | IllegalStateException ignored) {
            //A not found could be thrown but will be interpreted as isOwnByUser is false, so association is not public.
            associationDto.setVisibilityType(VisibilityType.PRIVATE);
        }
        return isOwnByUser && associationDto.getVisibilityType() == VisibilityType.PUBLIC;
    }

    private List<AssociationDto> mapSqlIntoAssociations(List<Tuple> associationsList) {
        return associationsList.stream()
                .map(associations -> new AssociationDto(
                        UUID.fromString((String) associations.get(0)),
                        (String) associations.get(1),
                        (String) associations.get(2),
                        (String) associations.get(3)))
                .toList();
    }

    private Subquery<UUID> getMonoClassSubquery(UUID id, CriteriaQuery<NamedQuery> query) {
        var monoClassSubquery = query.subquery(UUID.class);
        var monoClassSearchRequest = monoClassSubquery.from(MonoClassSearchRequest.class);
        var monoOclasses = monoClassSearchRequest.join(MonoClassSearchRequest_.oClass).get(OClass_.id);

        // Selects UUID from monoClassSearchRequest
        monoClassSubquery.select(monoClassSearchRequest.get(MonoClassSearchRequest_.id));
        // Only when oclass identifier match given id
        monoClassSubquery.where(monoOclasses.in(id));
        return monoClassSubquery;
    }

    private Subquery<UUID> getMultiClassSubquery(UUID id, CriteriaQuery<NamedQuery> query) {
        var multiClassSubquery = query.subquery(UUID.class);
        var multiClassSearchRequest = multiClassSubquery.from(MultiClassSearchRequest.class);
        var multiOclasses = multiClassSearchRequest.join(MultiClassSearchRequest_.oClasses);

        // Selects UUID from multiClassSearchRequest
        multiClassSubquery.select(multiClassSearchRequest.get(MultiClassSearchRequest_.id));
        // Only when oclass identifier match given id
        multiClassSubquery.where(multiOclasses.in(id));
        return multiClassSubquery;
    }

    private List<AssociationDto> getNamedQueryAssociationsForClass(UUID id) {
        // <1> Joins namedquerie  with search requests
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(NamedQuery.class);
        var namedQuery = query.from(NamedQuery.class).join(NamedQuery_.request);

        // <2> Joins mono class search requests  with oclass
        Subquery<UUID> monoClassSubquery = getMonoClassSubquery(id, query);
        // <3> Joins multi class search requests  with oclass
        Subquery<UUID> multiClassSubquery = getMultiClassSubquery(id, query);

        // Restricts <1> with <2> and <3>
        query.where(cb.or(
                cb.in(namedQuery.get(SearchRequest_.ID)).value(monoClassSubquery),
                cb.in(namedQuery.get(SearchRequest_.id)).value(multiClassSubquery)));
        return em.createQuery(query).getResultList()
                .stream()
                .map(item -> new AssociationDto(item.getId(), item.getName(), item.getVisibilityType(),
                        AssociationsType.NAMED_QUERY))
                .toList();
    }
}
