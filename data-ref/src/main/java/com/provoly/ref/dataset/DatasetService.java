package com.provoly.ref.dataset;

import static com.provoly.ref.groups.WithGroupEntityType.DATASET;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.user.SystemGroup;
import com.provoly.common.user.UserDto;
import com.provoly.ref.dashboard.Dashboard;
import com.provoly.ref.dashboard.Dashboard_;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityNamed_;
import com.provoly.ref.entity.GrantService;
import com.provoly.ref.groups.Group;
import com.provoly.ref.groups.GroupErrors;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.model.AssociationDto;
import com.provoly.ref.model.AssociationService;
import com.provoly.ref.model.AssociationsDto;
import com.provoly.ref.model.AssociationsType;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.widget.WidgetCatalog;
import com.provoly.ref.widget.WidgetCatalog_;

import org.jboss.logging.Logger;

import com.speedment.jpastreamer.application.JPAStreamer;

@ApplicationScoped
public class DatasetService {

    private EntityManager em;
    private DatasetVersionService datasetVersionService;
    private EntityIdService entityIdService;
    private AssociationService associationService;
    private JPAStreamer jpaStreamer;
    private DatasetVersionRepository datasetVersionRepository;
    private GroupService groupService;
    private Logger log;
    private DatasetMapper datasetMapper;
    private UserService userService;
    private GrantService grantService;

    public DatasetService(EntityManager em, DatasetVersionService datasetVersionService,
            EntityIdService entityIdService, AssociationService associationService,
            DatasetVersionRepository datasetVersionRepository, GroupService groupService,
            DatasetMapper datasetMapper, JPAStreamer jpaStreamer, Logger log, UserService userService,
            GrantService grantService) {
        this.em = em;
        this.datasetVersionService = datasetVersionService;
        this.entityIdService = entityIdService;
        this.associationService = associationService;
        this.datasetVersionRepository = datasetVersionRepository;
        this.jpaStreamer = jpaStreamer;
        this.groupService = groupService;
        this.log = log;
        this.userService = userService;
        this.datasetMapper = datasetMapper;
        this.grantService = grantService;
    }

    @Transactional
    public void save(DatasetDto datasetDto) {
        var dataset = datasetMapper.toModel(datasetDto);
        Optional<DatasetVersion> datasetHolder = Optional.empty();
        checkCanCreate(dataset);
        ProvolyUser currentUser = userService.getCurrentUser();

        if (!entityIdService.exists(dataset) && (dataset.getType() != DatasetType.CLOSED)) {
            var datasetVersion = new DatasetVersion(UUID.randomUUID());
            datasetVersion.setDataset(dataset);
            datasetVersion.setState(DatasetState.ACTIVE);
            datasetHolder = Optional.of(datasetVersion);
        }
        dataset.setUser(currentUser);
        entityIdService.saveEntity(dataset, false);
        groupService.updateEntityGroups(datasetDto.getGroups(), dataset.getId(), DATASET);
        datasetHolder.ifPresent(datasetVersionService::createDatasetVersion);
    }

    private void checkCanCreate(Dataset dataset) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var metadataRoot = q.from(Dataset.class);
        q.select(cb.count(metadataRoot));
        q.where(
                cb.equal(metadataRoot.get(Dataset_.O_CLASS), dataset.getoClass()),
                cb.equal(metadataRoot.get(EntityNamed_.NAME), dataset.getName()));
        var result = em.createQuery(q).getSingleResult();
        if (result > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Name %s already exists for class %s".formatted(dataset.getName(), dataset.getoClass().getName()));
        }
    }

    @Transactional
    public Dataset searchByDatasetVersionId(UUID datasetVersionId) {
        try {
            return datasetVersionRepository.getById(datasetVersionId).getDataset();
        } catch (NoResultException e) {
            throw new ProvolyNotFoundException("No dataset version with id %s exists".formatted(datasetVersionId));
        }
    }

    @Transactional
    public void deleteDataset(UUID id) {
        if (!canDeleteDataset(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "You're not allowed to delete dataset %s because it owns one or more dataset version".formatted(id));
        }
        entityIdService.removeEntity(id, Dataset.class);
    }

    public Collection<Dataset> getAllClassAllowedDatasets(UUID id) {
        ProvolyUser user = userService.getCurrentUser();
        log.infof("Get datasets for user %s with groups %s", user.getId(),
                user.getGroups().stream().map(Group::getName).toList());

        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT dataset.id FROM dataset " +
                        "LEFT JOIN group_relations as gr ON dataset.id = gr.entity_id " +
                        "WHERE gr.group_id in :groups_id OR dataset.user_id = :user_id ) " +
                        "SELECT * FROM dataset where o_class_id = :oclass_id AND id in (SELECT id FROM ids)",
                Dataset.class)
                .setParameter("user_id", user.getId())
                .setParameter("groups_id", user.getGroups().stream().map(Group::getId).toList())
                .setParameter("oclass_id", id)
                .getResultList();
    }

    public Dataset getByName(String name) {
        UserDto currentUserDto = userService.getCurrentUserDto();

        return jpaStreamer.stream(Dataset.class)
                .filter(dataset -> dataset.getName().equals(name)) // should use metamodel, but is blocked by https://github.com/speedment/jpa-streamer/issues/391
                .findFirst()
                .flatMap(dataset -> grantService.canSee(dataset, DATASET, currentUserDto) ? Optional.of(dataset)
                        : Optional.empty())
                .orElseThrow(() -> new ProvolyNotFoundException("Dataset %s doesn't exists".formatted(name)));
    }

    @Transactional
    public GroupErrors updateDataset(DatasetDto datasetDto) {
        var dataset = datasetMapper.toModel(datasetDto);
        var oldDataset = getById(dataset.getId());

        dataset.setUser(oldDataset.getUser());
        if (!oldDataset.canUpdateTo(dataset)) {
            throw new BusinessException(ErrorCode.NOT_MODIFIABLE, "The dataset's type is immutable.");
        }
        entityIdService.saveEntity(dataset);
        groupService.updateEntityGroups(datasetDto.getGroups(), dataset.getId(), DATASET);
        return getGroupsError(datasetDto);
    }

    private GroupErrors getGroupsError(DatasetDto dataset) {
        List<Group> groups = groupService.getGroupByNames(dataset.getGroups());
        Collection<UUID> dashboardIds = findDashboardsAssociationByDatasetId(dataset.getId()).stream()
                .map(AssociationDto::getId).toList();
        Map<UUID, Set<String>> groupsErrors = new HashMap<>();

        dashboardIds.forEach(dashboardId -> {
            List<Group> dashboardGroups = groupService.getGroupsByEntityId(dashboardId).stream().toList();
            getDashboardInconsistentGroups(dashboardGroups, groups)
                    .forEach(group -> groupsErrors.computeIfAbsent(dashboardId, ignored -> new HashSet<>()).add(group));
        });
        return new GroupErrors(groupsErrors);
    }

    private Set<String> getDashboardInconsistentGroups(List<Group> dashboardGroups, List<Group> datasetGroups) {
        if (datasetGroups.isEmpty()) {
            return dashboardGroups.stream().map(Group::getName).collect(Collectors.toSet());
        }
        List<UUID> datasetGroupIds = datasetGroups.stream().map(Group::getId).toList();

        if (datasetGroupIds.contains(SystemGroup.ALL.getId())) {
            return Set.of();
        }
        List<UUID> dashboardGroupIds = dashboardGroups.stream().map(Group::getId).toList();
        if (datasetGroupIds.contains(SystemGroup.AUTHENTICATED.getId())
                && dashboardGroupIds.contains(SystemGroup.ALL.getId())) {
            return Set.of(SystemGroup.ALL.name());
        }
        if (!dashboardGroupIds.contains(SystemGroup.AUTHENTICATED.getId())
                && !dashboardGroupIds.contains(SystemGroup.ALL.getId())) {
            List<Group> dashboardGroupCopy = new ArrayList<>(dashboardGroups);
            dashboardGroupCopy.removeAll(datasetGroups);
            return dashboardGroupCopy.stream().map(Group::getName).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public Dataset getById(UUID datasetId) {
        UserDto currentUserDto = userService.getCurrentUserDto();
        return jpaStreamer.stream(Dataset.class)
                .filter(dataset -> dataset.getId().equals(datasetId)) // should use metamodel, but is blocked by https://github.com/speedment/jpa-streamer/issues/391
                .findFirst()
                .flatMap(dataset -> grantService.canSee(dataset, DATASET, currentUserDto) ? Optional.of(dataset)
                        : Optional.empty())
                .orElseThrow(() -> new ProvolyNotFoundException("Dataset : %s inexistant.".formatted(datasetId)));

    }

    public Dataset findById(UUID datasetId) {
        UserDto currentUserDto = userService.getCurrentUserDto();

        return jpaStreamer.stream(Dataset.class)
                .filter(dataset -> dataset.getId().equals(datasetId))
                .filter(dataset -> grantService.canSee(dataset, DATASET, currentUserDto))
                .findFirst()
                .orElse(null);
    }

    public List<Dataset> getAll() {
        ProvolyUser user = userService.getCurrentUser();

        return em.createNativeQuery(
                "WITH ids AS (SELECT DISTINCT dataset.id FROM dataset " +
                        "LEFT JOIN group_relations as gr ON dataset.id = gr.entity_id " +
                        "WHERE gr.group_id in :groups_id OR dataset.user_id = :user_id ) " +
                        "SELECT * FROM dataset WHERE id in (SELECT id FROM ids)",
                Dataset.class)
                .setParameter("user_id", user.getId())
                .setParameter("groups_id", user.getGroups().stream().map(Group::getId).toList())
                .getResultList();
    }

    @Transactional
    public void saveEntity(Dataset entity) {
        entityIdService.saveEntity(entity);
    }

    public boolean exists(Dataset entity) {
        return entityIdService.exists(entity);
    }

    public AssociationsDto getDatasetAssociations(UUID datasetId) {
        getById(datasetId);
        List<AssociationDto> result = new ArrayList<>();
        result.addAll(findDashboardsAssociationByDatasetId(datasetId));
        result.addAll(findWidgetsByDatasetId(datasetId));
        return associationService.mapAssociationDtoInAssociationsDto(result);
    }

    public Collection<UUID> getAllFilterByDatasource(Collection<UUID> datasource) {
        //We need to filter the datasource on dataset because groups are implemented only on dataset
        return jpaStreamer.stream(Dataset.class)
                .map(EntityId::getId)
                .filter(datasource::contains)
                .toList();
    }

    private boolean canDeleteDataset(UUID datasetId) {
        try {
            return datasetVersionRepository.getByDatasetId(datasetId) == null;
        } catch (BusinessException error) {
            if (error.getCode().equals(ErrorCode.NOT_FOUND)) {
                return true;
            }
            throw error;
        }
    }

    public List<AssociationDto> findDashboardsAssociationByDatasetId(UUID datasetId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Dashboard> criteriaQuery = cb.createQuery(Dashboard.class);
        Root<Dashboard> dashboardRoot = criteriaQuery.from(Dashboard.class);
        Join<Dashboard, UUID> dataSourceJoin = dashboardRoot.join(Dashboard_.datasource);
        Predicate dataSourceIdPredicate = cb.equal(dataSourceJoin, datasetId);
        criteriaQuery.select(dashboardRoot)
                .where(dataSourceIdPredicate);
        return em.createQuery(criteriaQuery).getResultList()
                .stream()
                .map(dataset -> new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.DASHBOARD))
                .toList();
    }

    private List<AssociationDto> findWidgetsByDatasetId(UUID datasetId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<WidgetCatalog> criteriaQuery = cb.createQuery(WidgetCatalog.class);
        Root<WidgetCatalog> widgetCatalogRoot = criteriaQuery.from(WidgetCatalog.class);
        Join<WidgetCatalog, UUID> dataSourceJoin = widgetCatalogRoot.join(WidgetCatalog_.datasource);
        Predicate dataSourceIdPredicate = cb.equal(dataSourceJoin, datasetId);
        criteriaQuery.select(widgetCatalogRoot)
                .where(dataSourceIdPredicate);
        return em.createQuery(criteriaQuery).getResultList()
                .stream()
                .map(dataset -> new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.WIDGET))
                .toList();
    }
}