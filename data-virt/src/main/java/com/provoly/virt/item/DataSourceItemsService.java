package com.provoly.virt.item;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.*;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.datasource.DataSourceType;
import com.provoly.common.datasource.Search;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.relation.RelationDto;
import com.provoly.common.search.*;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.datasource.FilterDto;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.SearchService;
import com.provoly.virt.search.mono.MonoClassSearchService;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataSourceItemsService {

    private Logger logger;
    private SearchService searchService;
    private MonoClassSearchService monoClassSearchService;
    private DatasetService datasetService;
    private DataSourceService dataSourceService;
    private ModelService modelService;
    private ProvolyUserService provolyUserService;
    private DatasetVersionService datasetVersionService;
    private DataVirtProperties dataVirtProperties;

    public DataSourceItemsService(Logger logger,
            SearchService searchService,
            MonoClassSearchService monoClassSearchService,
            @RestClient DatasetService datasetDefinitionService,
            @RestClient ModelService modelService,
            @RestClient DataSourceService dataSourceService,
            @RestClient ProvolyUserService provolyUserService,
            @RestClient DatasetVersionService datasetVersionService,
            DataVirtProperties dataVirtProperties) {
        this.logger = logger;
        this.searchService = searchService;
        this.monoClassSearchService = monoClassSearchService;
        this.datasetService = datasetDefinitionService;
        this.dataSourceService = dataSourceService;
        this.modelService = modelService;
        this.provolyUserService = provolyUserService;
        this.datasetVersionService = datasetVersionService;
        this.dataVirtProperties = dataVirtProperties;
    }

    public AggregationResultDto getAggregationResult(UUID datasourceId, AggregationParamDto aggregation,
            List<FilterDto> filters, boolean excludeGeo, int limit) {
        logger.infof("Start aggregation on datasource %s and with param %s ", datasourceId, aggregation.toString());
        if (limit < 0 || limit > dataVirtProperties.maxSizeLimit()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Limit can't be negative or exceed %s.".formatted(dataVirtProperties.maxSizeLimit()));
        }
        if (aggregation.operation() != null && aggregation.operation() != AggregateOperation.COUNT
                && aggregation.valueField() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "valueField is missing for operation %s".formatted(aggregation.operation()));
        }
        if (aggregation.sortAggregates() != null && aggregation.groupBy() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Sort is not available when a groupBy has been set");
        }
        DataSourceDetailsDto datasource = dataSourceService.getDataSourceDetails(datasourceId);
        var request = getSearchRequest(datasource, excludeGeo, limit, null, false, false);

        if (request instanceof MultiClassRequestDto) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Namedquery %s is not a %s".formatted(datasourceId, SearchRequestType.MONO_CLASS));
        }
        updateRequestWithFilters(request, filters);
        return monoClassSearchService.aggregate(aggregation, (MonoClassRequestDto) request);
    }

    public ItemsSearchResult getItems(UUID dataSourceId, SortDto sort, List<FilterDto> filters, SearchRequestDto requestDto) {
        return switch (requestDto) {
            case MonoClassRequestDto monoClassRequestDto ->
                getItems(dataSourceId, sort, filters, requestDto.getLimit(), requestDto.isExcludeGeo(),
                        monoClassRequestDto.getCondition(), requestDto.isWithSourceItems(),
                        requestDto.isWithDestinationItems(), requestDto.getWithRelation(),
                        monoClassRequestDto.getSearchAfter());
            case MultiClassRequestDto multiClassRequestDto ->
                getItems(dataSourceId, sort, filters, requestDto.getLimit(), requestDto.isExcludeGeo(), null,
                        requestDto.isWithSourceItems(), requestDto.isWithDestinationItems(), requestDto.getWithRelation(),
                        null);
        };
    }

    public ItemsSearchResult getItems(UUID dataSourceId, SortDto sort, List<FilterDto> filters, int limit, boolean excludeGeo,
            ConditionDto conditionDto, boolean withSourceItems, boolean withDestinationItems) {
        return getItems(dataSourceId, sort, filters, limit, excludeGeo, conditionDto, withSourceItems, withDestinationItems,
                null, null);
    }

    //TODO change this method to turn it private or prevent from being called with a previous SearchRequest then building a new one
    public ItemsSearchResult getItems(UUID dataSourceId, SortDto sort, List<FilterDto> filters, int limit, boolean excludeGeo,
            ConditionDto conditionDto, boolean withSourceItems, boolean withDestinationItems, RelationDto withRelation,
            String searchAfter) {
        DataSourceDetailsDto datasource = dataSourceService.getDataSourceDetails(dataSourceId);
        SearchRequestDto request = getSearchRequest(datasource, excludeGeo, limit, conditionDto, withSourceItems,
                withDestinationItems, withRelation, searchAfter);

        updateRequestWithFilters(request, filters);
        ItemsSearchResult result = searchService.search(request, sort);
        if (datasource.type() == DataSourceType.SEARCH) {
            provolyUserService.updateNamedQueryExecution(dataSourceId);
        }
        return result;
    }

    public List<String> searchForAttributeValues(Search search) {
        // Required in #28 to only get the first value for now
        var datasourceByAttribute = search.attributes().getFirst();
        var dataSourceDetails = dataSourceService.getDataSourceDetails(datasourceByAttribute.getDatasource());
        DatasetVersionDetailsDto datasetVersionDto = switch (dataSourceDetails.type()) {
            case DATASET_VERSION -> datasetVersionService.get(datasourceByAttribute.getDatasource());
            case DATASET -> datasetService.getDatasetVersionByDatasetId(datasourceByAttribute.getDatasource());
            case SEARCH ->
                throw new BusinessException(ErrorCode.NOT_SUPPORTED, "Autocomplete is not implemented on namedquery yet.");
        };
        return searchForAttributeValue(datasourceByAttribute.getAttribute(), datasetVersionDto, search.value(),
                search.limit());
    }

    private SearchRequestDto getSearchRequest(DataSourceDetailsDto datasource, boolean excludeGeo, int limit,
            ConditionDto conditionDto, boolean withSourceItems, boolean withDestinationItems) {
        return getSearchRequest(datasource, excludeGeo, limit, conditionDto, withSourceItems, withDestinationItems, null, null);
    }

    private SearchRequestDto getSearchRequest(DataSourceDetailsDto datasource, boolean excludeGeo, int limit,
            ConditionDto conditionDto, boolean withSourceItems, boolean withDestinationItems, RelationDto withRelation,
            String searchAfter) {
        if (limit == 0) {
            limit = dataVirtProperties.searchLimit();
        }
        SearchRequestDto request = switch (datasource.type()) {
            case SEARCH -> provolyUserService.getNamedQueryById(datasource.id()).getRequest();
            case DATASET_VERSION ->
                new MonoClassRequestDto(datasource.oClass(), List.of(datasource.id()), excludeGeo, limit, conditionDto);
            case DATASET -> buildMonoClassRequestWithDataset(datasource, excludeGeo, limit, conditionDto);
        };

        request.setExcludeGeo(excludeGeo);
        request.setLimit(limit);
        request.setWithSourceItems(withSourceItems);
        request.setWithDestinationItems(withDestinationItems);
        request.setWithRelation(withRelation);
        if (request instanceof MonoClassRequestDto monoRequest) {
            monoRequest.setSearchAfter(searchAfter);
        }

        return request;
    }

    private MonoClassRequestDto buildMonoClassRequestWithDataset(DataSourceDetailsDto datasource, boolean excludeGeo,
            int limit, ConditionDto conditionDto) {
        try {
            DatasetVersionDetailsDto datasetVersion = datasetService.getDatasetVersionByDatasetId(datasource.id());
            return new MonoClassRequestDto(datasource.oClass(), List.of(datasetVersion.getId()), excludeGeo, limit,
                    conditionDto);
        } catch (BusinessException e) {
            logger.warn("No dataset available for dataset version %s.".formatted(datasource.id()));
            return new MonoClassRequestDto(datasource.oClass(), List.of(), excludeGeo, limit, conditionDto);
        }
    }

    private void updateRequestWithFilters(SearchRequestDto request, List<FilterDto> filters) {
        AndConditionDto andCondition = new AndConditionDto();
        if (filters != null && !filters.isEmpty()) {
            if (request instanceof MultiClassRequestDto) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Filtering on multi class request is not available");
            }
            var monoRequest = (MonoClassRequestDto) request;
            var oClass = modelService.getDetails(monoRequest.getoClass());
            filters.forEach(filter -> andCondition.composed.add(convertFilterToCondition(oClass, filter)));

            if (monoRequest.getCondition() != null) {
                logger.debug("Merge condition from SearchRequest with the new conditions from filters");
                andCondition.composed.add(monoRequest.getCondition());
            }
            monoRequest.setCondition(andCondition);
        }
    }

    private ConditionDto convertFilterToCondition(OClassDetailsDto oClass, FilterDto filter) {
        if (!isIdAttributeBelongToOClass(filter, oClass)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Attribute %s doesn't belong to oClass %s".formatted(filter.attribute(), oClass.getId()));
        }
        var orCondition = new OrConditionDto();

        if (filter.operator().isWithUpperValue()) {
            orCondition.composed
                    .add(new AttributeConditionDto(filter.attribute(), filter.value(), filter.operator(), filter.upperValue()));
        } else if (filter.operator().isWithNativeListOfValues()) {
            String expressionLanguageForListOfValues = "${ ["
                    + filter.values().stream().map(value -> "'" + value + "'").collect(Collectors.joining(",")) + "] }";
            orCondition.composed
                    .add(new AttributeConditionDto(filter.attribute(), expressionLanguageForListOfValues, null, null, null,
                            filter.operator()));
        } else {
            filter.values().forEach(
                    value -> orCondition.composed.add(new AttributeConditionDto(filter.attribute(), value, filter.operator())));
        }

        return orCondition;
    }

    private boolean isIdAttributeBelongToOClass(FilterDto f, OClassDetailsDto oClass) {
        return oClass.getAttributes().stream().anyMatch(attributeDefDto -> attributeDefDto.getId().equals(f.attribute()));
    }

    private List<String> searchForAttributeValue(UUID attributeId,
            DatasetVersionDetailsDto datasetVersion, String search, Integer limit) {
        OClassDetailsDto oClassDto = modelService.getDetails(datasetVersion.getoClass());

        AttributeDefDetailsDto attributesFormatted = oClassDto.getAttributes()
                .stream()
                .filter(att -> att.getId().equals(attributeId))
                .findFirst()
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.BAD_REQUEST,
                                "AttributeId %s is unknown for class %s".formatted(attributeId, oClassDto.getId())));
        checkValidAttribute(attributesFormatted);
        SortAggregate sortAggregate = new SortAggregate(Direction.asc, OrderBy.KEY);
        AggregationParamDto params = new AggregationParamDto(attributesFormatted.getId(), AggregateOperation.COUNT, null,
                sortAggregate);
        FilterDto filterDto = new FilterDto(attributesFormatted.getId(), Operator.I_CONTAINS, search);
        if (limit == null) {
            limit = dataVirtProperties.maxSizeLimit();
        }
        return extractAttributesValues(getAggregationResult(datasetVersion.getId(), params, List.of(filterDto), true, limit));
    }

    private static void checkValidAttribute(AttributeDefDetailsDto attributesFormatted) {
        if (attributesFormatted.getField().getType() != Type.KEYWORD) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Search only implemented on attribute with type keyword.");
        }
        if (attributesFormatted.isMultiValued()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Search only implemented on simple value attribute.");
        }
    }

    private List<String> extractAttributesValues(AggregationResultDto itemsSearchResult) {
        return itemsSearchResult.values()
                .stream()
                .map(ItemAggregationDto::getKey)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }
}
