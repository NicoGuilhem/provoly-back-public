package com.provoly.ref.dataset;

import static com.provoly.ref.groups.WithGroupEntityType.DATASET;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.common.model.CategoryDto;
import com.provoly.common.user.SystemGroup;
import com.provoly.ref.category.*;
import com.provoly.ref.datasetversion.DatasetVersion;
import com.provoly.ref.datasetversion.DatasetVersionRepository;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.groups.*;
import com.provoly.ref.model.AssociationDto;
import com.provoly.ref.model.AssociationService;
import com.provoly.ref.model.AssociationsDto;
import com.provoly.ref.model.AssociationsType;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.user.VisibilityType;

@ApplicationScoped
public class DatasetService {

    private DatasetVersionService datasetVersionService;
    private AssociationService associationService;
    private DatasetVersionRepository datasetVersionRepository;
    private GroupService groupService;
    private GroupRepository groupRepository;
    private DatasetMapper datasetMapper;
    private UserService userService;
    private GrantService grantService;
    private DatasetRepository datasetRepository;
    private CategoryService categoryService;

    public DatasetService(DatasetVersionService datasetVersionService, AssociationService associationService,
            DatasetVersionRepository datasetVersionRepository, GroupService groupService, GroupRepository groupRepository,
            DatasetMapper datasetMapper, UserService userService,
            GrantService grantService, DatasetRepository datasetRepository,
            CategoryService categoryService) {
        this.datasetVersionService = datasetVersionService;
        this.associationService = associationService;
        this.datasetVersionRepository = datasetVersionRepository;
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.datasetMapper = datasetMapper;
        this.grantService = grantService;
        this.datasetRepository = datasetRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public void save(DatasetDto datasetDto) {
        var dataset = datasetMapper.toModel(datasetDto);
        Optional<DatasetVersion> datasetHolder = Optional.empty();
        checkCanCreate(dataset);
        ProvolyUser currentUser = userService.getCurrentUser();

        if (!datasetRepository.exists(dataset) && (dataset.getType() != DatasetType.CLOSED)) {
            var datasetVersion = new DatasetVersion(UUID.randomUUID());
            datasetVersion.setDataset(dataset);
            datasetVersion.setState(DatasetState.ACTIVE);
            datasetHolder = Optional.of(datasetVersion);
        }
        dataset.setUser(currentUser);
        datasetRepository.save(dataset, false);
        if (datasetDto.getGroups() != null) {
            var rightsByGroup = datasetDto.getGroups().stream()
                    .collect(Collectors.toMap(groupName -> groupName, _ignored -> List.of(
                            GroupRights.READ)));
            groupService.updateEntityGroups(rightsByGroup, dataset.getId(), DATASET);
        }
        if (datasetDto.getCategories() != null) {
            categoryService.updateEntityCategories(datasetDto.getCategories(), dataset.getId(), WithCategoryEntityType.DATASET);
        }
        datasetHolder.ifPresent(datasetVersionService::createDatasetVersion);
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
        var dataset = datasetRepository.getById(id).orElseThrow(() -> new ProvolyNotFoundException(Dataset.class, id));
        grantService.canWrite(dataset, DATASET, userService.getCurrentUser());

        if (isDatasetAssociateToDatasetVersion(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "You're not allowed to delete dataset %s because it owns one or more dataset version".formatted(id));
        }
        datasetRepository.delete(id);
    }

    public Dataset getByName(String name) {
        ProvolyUser currentUserDto = userService.getCurrentUser();
        return datasetRepository.getByName(name)
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
        datasetRepository.save(dataset);
        var rightsByGroup = datasetDto.getGroups() == null ? null
                : datasetDto.getGroups().stream()
                        .collect(Collectors.toMap(groupName -> groupName, _ignored -> List.of(
                                GroupRights.READ)));
        groupService.updateEntityGroups(rightsByGroup, dataset.getId(), DATASET);
        if (datasetDto.getCategories() != null) {
            categoryService.updateEntityCategories(datasetDto.getCategories(), dataset.getId(), WithCategoryEntityType.DATASET);
        }
        return getGroupsError(datasetDto);
    }

    public Dataset getById(UUID datasetId) {
        ProvolyUser currentUserDto = userService.getCurrentUser();
        return datasetRepository.getById(datasetId)
                .flatMap(dataset -> grantService.canSee(dataset, DATASET, currentUserDto) ? Optional.of(dataset)
                        : Optional.empty())
                .orElseThrow(() -> new ProvolyNotFoundException("Dataset : %s inexistant.".formatted(datasetId)));

    }

    public Dataset findById(UUID datasetId) {
        ProvolyUser currentUser = userService.getCurrentUser();
        return datasetRepository.getById(datasetId).filter(dataset -> grantService.canSee(dataset, DATASET, currentUser))
                .orElse(null);
    }

    public List<Dataset> getAll() {
        var user = userService.getCurrentUser();
        return grantService.getAllUserAllowed(DATASET, user);
    }

    public AssociationsDto getDatasetAssociations(UUID datasetId) {
        getById(datasetId);
        List<AssociationDto> result = new ArrayList<>();
        result.addAll(findDashboardsAssociationByDatasetId(datasetId));
        result.addAll(findWidgetsByDatasetId(datasetId));
        return associationService.mapAssociationDtoInAssociationsDto(result);
    }

    public List<AssociationDto> findDashboardsAssociationByDatasetId(UUID datasetId) {
        return datasetRepository.findAssociatedDashboards(datasetId)
                .stream()
                .map(dataset -> new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.DASHBOARD))
                .toList();
    }

    public Collection<Dataset> getAllClassAllowedDatasets(UUID id) {
        ProvolyUser provolyUser = userService.getCurrentUser();
        return grantService.getUserAllowedDatasetsByClass(provolyUser, id);
    }

    public void addCategory(CategoryDto categoryDto) {
        Category category = new Category(categoryDto.id(), categoryDto.name(), WithCategoryEntityType.DATASET);
        categoryService.save(category);
    }

    private List<AssociationDto> findWidgetsByDatasetId(UUID datasetId) {
        return datasetRepository.findAssociatedWidget(datasetId)
                .stream()
                .map(dataset -> new AssociationDto(dataset.getId(), dataset.getName(), VisibilityType.PUBLIC,
                        AssociationsType.WIDGET))
                .toList();
    }

    private boolean isDatasetAssociateToDatasetVersion(UUID datasetId) {
        return !datasetVersionRepository.getAllByDatasetId(datasetId).isEmpty();
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

    private GroupErrors getGroupsError(DatasetDto dataset) {
        List<Group> groups = groupRepository.getGroupByNames(dataset.getGroups());
        Collection<UUID> dashboardIds = findDashboardsAssociationByDatasetId(dataset.getId()).stream()
                .map(AssociationDto::getId).toList();
        Map<UUID, Set<String>> groupsErrors = new HashMap<>();

        dashboardIds.forEach(dashboardId -> {
            List<Group> dashboardGroups = groupRepository.getGroupsByEntityId(dashboardId).stream()
                    .map(GroupRelations::getGroup)
                    .toList();
            getDashboardInconsistentGroups(dashboardGroups, groups)
                    .forEach(group -> groupsErrors.computeIfAbsent(dashboardId, ignored -> new HashSet<>()).add(group));
        });
        return new GroupErrors(groupsErrors);
    }

    private void checkCanCreate(Dataset dataset) {
        if (datasetRepository.isNameAlreadyExistForClass(dataset)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Name %s already exists for class %s".formatted(dataset.getName(), dataset.getoClass().getName()));
        }
    }
}