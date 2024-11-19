package com.provoly.ref.user;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.*;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.searchrequest.MonoClassSearchRequest;
import com.provoly.ref.searchrequest.MonoClassSearchRequest_;
import com.provoly.ref.searchrequest.MultiClassSearchRequest;
import com.provoly.ref.searchrequest.MultiClassSearchRequest_;
import com.provoly.ref.widget.WidgetCatalog;

import org.jboss.logging.Logger;

@ApplicationScoped
public class NamedQueryService {

    private Logger log;

    private UserService userService;

    private NamedQueryMapper mapper;

    private EntityIdRepository entityIdRepository;

    private SearchMapper searchMapper;
    private DatasetVersionRepository datasetVersionRepository;

    @PersistenceContext
    EntityManager em;

    public NamedQueryService(Logger log, UserService userService, NamedQueryMapper mapper,
            EntityIdRepository entityIdRepository,
            SearchMapper searchMapper, DatasetVersionRepository datasetVersionRepository) {
        this.log = log;
        this.userService = userService;
        this.mapper = mapper;
        this.entityIdRepository = entityIdRepository;
        this.searchMapper = searchMapper;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    public Collection<ProvolyUserNamedQuery> getNamedQueriesForCurrentUser() {
        log.info("Getting all NamedQuery for user");
        var currentUser = userService.getCurrentUser();

        return getNamedQueriesForUser(currentUser);
    }

    private Collection<ProvolyUserNamedQuery> getNamedQueriesForUser(ProvolyUser user) {

        var cb = em.getCriteriaBuilder();
        var namedQuery = cb.createQuery(NamedQuery.class);
        var namedQueryRoot = namedQuery.from(NamedQuery.class);

        namedQuery.where(cb.or(
                isLevelPublic(cb, namedQueryRoot),
                isBelongTo(cb, namedQuery, namedQueryRoot, user)));

        List<NamedQuery> namedQueries = em.createQuery(namedQuery).getResultList();
        return namedQueries.stream().map(nq -> nq.getForUser(user)).toList();
    }

    /**
     * Build a predicate checking if NamedQuery is public
     */
    private Predicate isLevelPublic(CriteriaBuilder cb, Root<NamedQuery> namedQueryRoot) {
        return cb.equal(namedQueryRoot.get(NamedQuery_.visibilityType), VisibilityType.PUBLIC);
    }

    private Predicate isBelongTo(CriteriaBuilder cb, CriteriaQuery<NamedQuery> namedQuery, Root<NamedQuery> namedQueryRoot,
            ProvolyUser currentUser) {
        // Build a sub query to check if a ProvolyUserNamedQuery is associated to the NamedQuery
        var subUserNamedQuery = namedQuery.subquery(Integer.class);
        var subUserNamedQueryRoot = subUserNamedQuery
                .correlate(namedQueryRoot)
                .join(NamedQuery_.belongTo);

        var isCurrentUser = cb.equal(subUserNamedQueryRoot.get(ProvolyUserNamedQuery_.user), currentUser);
        var isOwner = cb.equal(subUserNamedQueryRoot.get(ProvolyUserNamedQuery_.owner), true);

        var subquery = subUserNamedQuery
                .select(cb.literal(1))
                .where(isCurrentUser, isOwner);
        return cb.exists(subquery);
    }

    @Transactional
    public void saveNamedQueryForUser(NamedQueryDto dto) {
        NamedQuery namedQuery = entityIdRepository.findById(dto.getId(), NamedQuery.class);
        checkAllDatasetVersionsExistInNamedquery(dto);
        var currentUser = userService.getCurrentUser();
        if (namedQuery == null) {
            namedQuery = mapper.toEntity(dto);
            em.persist(namedQuery);
        } else {
            // only creator can udpate namedquery
            checkCurrentUserOwnsNamedquery(namedQuery);
            mapper.update(dto, namedQuery);
            // It's not possible to use mapstruct for requests because the @SubClassMapping is not available in the case of an update : https://github.com/mapstruct/mapstruct/issues/3126
            updateSearchRequest(dto.getRequest());
        }
        namedQuery.add(currentUser, true);
        namedQuery.setFavoriteFor(currentUser, dto.isFavorite());
        namedQuery.setColorFor(currentUser, dto.getColor());
    }

    private void updateSearchRequest(SearchRequestDto requestDto) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

        switch (requestDto) {
            case MonoClassRequestDto e -> {
                CriteriaQuery<MonoClassSearchRequest> query = criteriaBuilder.createQuery(MonoClassSearchRequest.class);
                var root = query.from(MonoClassSearchRequest.class);
                var q = query.select(root)
                        .where(criteriaBuilder.equal(root.get(MonoClassSearchRequest_.id), e.getId()));
                MonoClassSearchRequest currentMonoRequest = em.createQuery(q).getSingleResult();
                MonoClassSearchRequest updatedMonoRequest = (MonoClassSearchRequest) searchMapper.toEntity(requestDto);

                currentMonoRequest.setCondition(updatedMonoRequest.getCondition());
                currentMonoRequest.setoClass(updatedMonoRequest.getoClass());
                currentMonoRequest.setSort(updatedMonoRequest.getSort());
            }
            case MultiClassRequestDto e -> {
                CriteriaQuery<MultiClassSearchRequest> query = criteriaBuilder.createQuery(MultiClassSearchRequest.class);
                var root = query.from(MultiClassSearchRequest.class);
                var q = query.select(root)
                        .where(criteriaBuilder.equal(root.get(MultiClassSearchRequest_.id), e.getId()));
                MultiClassSearchRequest currentMultiRequest = em.createQuery(q).getSingleResult();
                MultiClassSearchRequest updatedMonoRequest = (MultiClassSearchRequest) searchMapper.toEntity(requestDto);

                currentMultiRequest.setFields(updatedMonoRequest.getFields());
                currentMultiRequest.setoClasses(updatedMonoRequest.getoClasses());
                currentMultiRequest.setMultiType(updatedMonoRequest.getMultiType());
            }
        }

    }

    @Transactional
    public ProvolyUserNamedQuery getMineById(UUID id) {
        var currentUser = userService.getCurrentUser();
        return getById(id).getForUser(currentUser);
    }

    @Transactional
    public void delete(UUID id) {
        var namedQuery = getById(id);
        checkCurrentUserOwnsNamedquery(namedQuery);
        checkNamedQueryNotUsedAndNotPublic(namedQuery);
        removeEntity(id, NamedQuery.class);
    }

    public <T extends EntityId> void removeEntity(UUID id, Class<T> entityClass) {
        entityIdRepository.removeEntity(id, entityClass);
        removeEntityAssociated(id);
    }

    public void removeNamedQueryIfExists(UUID id) {
        entityIdRepository.removeIfExists(id, NamedQuery.class);
    }

    private void removeEntityAssociated(UUID namedQueryId) {
        removeProvolyUserNamedQuery(namedQueryId);
    }

    private void checkAllDatasetVersionsExistInNamedquery(NamedQueryDto dto) {
        if (dto.getRequest().getType() == SearchRequestType.MONO_CLASS) {
            MonoClassRequestDto monoClassRequestDto = (MonoClassRequestDto) dto.getRequest();
            if (monoClassRequestDto.getDatasetVersionIds() != null) {
                monoClassRequestDto.getDatasetVersionIds()
                        .forEach(datasetVersionId -> datasetVersionRepository.getById(datasetVersionId));
            }
        }
    }

    private void removeProvolyUserNamedQuery(UUID namedQueryId) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaDelete<ProvolyUserNamedQuery> deleteCriteria = criteriaBuilder
                .createCriteriaDelete(ProvolyUserNamedQuery.class);
        Root<ProvolyUserNamedQuery> root = deleteCriteria.from(ProvolyUserNamedQuery.class);
        deleteCriteria
                .where(criteriaBuilder.equal(root.get(ProvolyUserNamedQuery_.NAMED_QUERY).get(NamedQuery_.ID), namedQueryId));
        em.createQuery(deleteCriteria).executeUpdate();
    }

    private void checkNamedQueryNotUsedAndNotPublic(NamedQuery namedQuery) {
        if (namedQuery.getVisibilityType() == VisibilityType.PUBLIC) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Namedquery %s is %s".formatted(namedQuery.getId(), VisibilityType.PUBLIC));
        }
        for (var w : entityIdRepository.getAll(WidgetCatalog.class)) {
            for (var datasource : w.getDatasource()) {
                if (datasource.equals(namedQuery.getId())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN,
                            "Namedquery %s is used in one or more widgets".formatted(namedQuery.getId()));
                }
            }
        }
    }

    private void checkCurrentUserOwnsNamedquery(NamedQuery namedQuery) {
        ProvolyUser user = userService.getCurrentUser();

        if (!namedQuery.isOwner(user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Namedquery %s not belong to user".formatted(namedQuery.getId()));
        }
    }

    @Transactional
    public void addToMineFavorite(UUID id) {
        getMineById(id).setFavorite(true);
    }

    @Transactional
    public void removeFromMineFavorite(UUID id) {
        getMineById(id).setFavorite(false);
    }

    @Transactional
    public void updateLastExecutionDate(UUID nameQueryId) {
        try {
            getMineById(nameQueryId).setLastExecutionDate(Instant.now());
        } catch (BusinessException ignored) {
            log.infof("It's not possible to update last execution date because namedquery not belong to current user");
        }
    }

    @Transactional
    public NamedQuery getById(UUID id) {
        return entityIdRepository.getById(id, NamedQuery.class);
    }

    @Transactional
    public NamedQuery findById(UUID id) {
        return entityIdRepository.findById(id, NamedQuery.class);
    }
}