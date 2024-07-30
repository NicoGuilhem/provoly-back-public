package com.provoly.virt.search.mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.AbacService;
import com.provoly.clients.DatasetVersionService;
import com.provoly.clients.ModelService;
import com.provoly.clients.PredicateService;
import com.provoly.common.Storage;
import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.Type;
import com.provoly.common.search.*;
import com.provoly.virt.ProvolySpanManager;
import com.provoly.virt.entity.AttributeMultiValue;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.storage.StorageAggregateAdapter;
import com.provoly.virt.storage.StorageRelationAdapters;
import com.provoly.virt.storage.StorageSearchAdapters;
import com.provoly.virt.storage.StorageSupport;

import io.opentelemetry.api.trace.Span;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MonoClassSearchService {
    private Logger log;
    private StorageRelationAdapters relationService;
    private ModelService modelService;
    private AbacService abacService;
    private PredicateService predicateService;
    private DatasetVersionService datasetVersionService;
    private PredicateEvaluator predicatEvaluator;
    private ConditionsEvaluator conditionsEvaluator;

    private ProvolySpanManager spanManager;
    private StorageAggregateAdapter storageAggregateAdapter;
    private StorageSearchAdapters storageSearchAdapters;
    private StorageSupport storageSupport;

    public MonoClassSearchService(Logger log,
            StorageRelationAdapters relationService,
            PredicateEvaluator predicateEvaluator,
            ConditionsEvaluator conditionsEvaluator,
            @RestClient ModelService modelService,
            @RestClient AbacService abacService,
            @RestClient PredicateService predicateService,
            @RestClient DatasetVersionService datasetVersionService,
            ProvolySpanManager spanManager,
            StorageAggregateAdapter storageAggregateAdapter,
            StorageSearchAdapters storageSearchAdapters, StorageSupport storageSupport) {
        this.log = log;
        this.relationService = relationService;
        this.modelService = modelService;
        this.abacService = abacService;
        this.predicateService = predicateService;
        this.datasetVersionService = datasetVersionService;
        this.predicatEvaluator = predicateEvaluator;
        this.conditionsEvaluator = conditionsEvaluator;
        this.spanManager = spanManager;
        this.storageAggregateAdapter = storageAggregateAdapter;
        this.storageSearchAdapters = storageSearchAdapters;
        this.storageSupport = storageSupport;
    }

    public AggregationResultDto aggregate(AggregationParamDto aggregation, MonoClassRequestDto request) {
        var classId = request.getoClass();
        var classDto = modelService.getDetails(classId);
        checkAggregationParameters(aggregation, classDto);

        MonoClassContextRequest monoClassContextRequest = initMonoClassContextRequest(request, classDto);

        if (monoClassContextRequest == null) {
            return new AggregationResultDto(aggregation.operation(), List.of());
        }
        AggregationResultDto result = storageAggregateAdapter.aggregate(classDto,
                request, aggregation,
                monoClassContextRequest);
        log.infof("Aggregation result size : %d", result.values().size());
        return result;
    }

    private void checkAggregationParameters(AggregationParamDto aggregation, OClassDetailsDto classDto) {
        checkAttributeIsNotStringType(aggregation.aggregatedBy(), classDto);
        checkAttributeIsNotStringType(aggregation.valueField(), classDto);
        checkAttributeIsNotStringType(aggregation.groupBy(), classDto);
    }

    private void checkAttributeIsNotStringType(UUID attributeId, OClassDetailsDto oClassDetailsDto) {
        if (attributeId != null) {
            var attribute = storageSupport.getAttributeDetail(oClassDetailsDto, attributeId);
            if (attribute.getField().getType() == Type.STRING) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "It's not possible to aggregate on string/text attribute.");
            }
        }
    }

    public ItemsSearchResult search(MonoClassRequestDto request) {
        UUID classId = request.getoClass();
        var start = Instant.now();
        log.infof("Init search on class %s", classId);

        var classDto = modelService.getDetails(classId);
        checkNotMixedAttributeClass(classDto, request);
        checkRequestedAttributes(classDto, request);
        MonoClassContextRequest monoClassContextRequest = initMonoClassContextRequest(request, classDto);

        var result = new ItemsSearchResult();

        if (monoClassContextRequest == null) {
            return result;
        }

        log.debugf("Start search on class %s", classDto.getName());
        Span spanSearch = spanManager.generateSpan("Start search",
                Map.of("request", request.toString(),
                        "context", monoClassContextRequest.toString()));

        try {
            result = storageSearchAdapters.search(classDto, request,
                    monoClassContextRequest);

            if (classDto.getStorage() == Storage.ELASTIC) {
                relationService.loadRelations(result);
            }

            result.getItems().forEach(item -> updateVisibility(item, monoClassContextRequest.securityMetaCondition()));

            log.infof("Search result size : %d", result.size());
        } catch (Exception e) {
            spanManager.recordException(spanSearch, e);
            throw e;
        } finally {
            log.infof(
                    "Search on %s took  - Duration %d",
                    classDto.getName(),
                    Duration.between(start, Instant.now()).toMillis());
            spanSearch.end();
        }

        return result;
    }

    private void checkRequestedAttributes(OClassDetailsDto classDto, MonoClassRequestDto request) {
        if (!request.getRequestedAttributes().isEmpty()) {
            var unrecognizedAttribute = request.getRequestedAttributes().stream()
                    .filter(id -> classDto.getAttributeById(id).isEmpty()).toList();
            if (!unrecognizedAttribute.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "No attribute " + unrecognizedAttribute + " belongs to class " + classDto.getName());
            }
        }
    }

    private MonoClassContextRequest initMonoClassContextRequest(MonoClassRequestDto request, OClassDetailsDto classDto) {

        List<AttributeDefDetailsDto> requestedAttributes = new ArrayList<>(classDto.getAttributes()
                .stream()
                .filter(attr -> request.getRequestedAttributes().contains(attr.getId())
                        || request.getRequestedAttributes().isEmpty())
                .toList());

        if (request.isExcludeGeo()) {
            requestedAttributes = requestedAttributes
                    .stream()
                    .filter(attr -> !attr.getField().getType().isGeo())
                    .toList();
        }

        if (requestedAttributes.isEmpty()) {
            log.info("No requested attributes : Skipping process");
            return null;
        }

        log.infof("Requested attributes are : %s", requestedAttributes);

        var monoClassContextRequest = new MonoClassContextRequest(new OrConditionDto(), new OrConditionDto(),
                getDatasetsCondition(request), requestedAttributes);

        if (monoClassContextRequest.datasetsCondition().composed.isEmpty()) {
            log.info("No dataset : Skipping process");
            return null;
        }

        Span span = spanManager.generateSpan("Add security",
                Map.of("request", request.toString()));

        try {
            addSecurity(classDto.getId(), request, monoClassContextRequest);
        } catch (Exception e) {
            spanManager.recordException(span, e);
        } finally {
            span.end();
        }
        return monoClassContextRequest;
    }

    private void addSecurity(UUID classId, MonoClassRequestDto request, MonoClassContextRequest monoClassContextRequest) {
        var rulesDto = abacService.getRuleFor(classId);
        log.debugf("Found %s rules for class, now adding all rules based on metadata (not associated with class).",
                rulesDto.size());
        rulesDto.addAll(abacService.getAllRules(AbacRuleType.METADATA));
        log.tracef("Rules for oclass %s : %s, total : %s", classId, rulesDto.size(), rulesDto);
        var context = abacService.listContextVariables();

        for (AbacRuleDto ruleDto : rulesDto) {
            if (!ruleDto.active) {
                log.tracef("Rule %s not active, continue", ruleDto.id);
                continue;
            }
            String expression = "${" + predicateService.getPredicate(ruleDto.predicate).value + "}";
            if (!predicatEvaluator.evaluate(expression, Boolean.class, context)) {
                ConditionDto computedCondition = ruleDto.condition;
                if (ruleDto.type == AbacRuleType.METADATA) {
                    log.trace("Build metadata security condition");
                    computedCondition = buildSecurityMetaCondition(computedCondition);
                    monoClassContextRequest.securityMetaCondition().composed.add(computedCondition);
                }
                log.tracef("Add security condition : %s", computedCondition);
                monoClassContextRequest.securityCondition().composed.add(computedCondition);
            }
        }

        if (request.getCondition() != null) {
            log.trace("Evaluate request conditions");
            // TODO : condition should be a TrueConditionDto instead of null
            predicatEvaluator.evaluate(request.getCondition(), context);
        }
        log.trace("Evaluate security condition");
        predicatEvaluator.evaluate(monoClassContextRequest.securityCondition(), context);
    }

    private ConditionDto buildSecurityMetaCondition(ConditionDto condition) {
        return switch (condition.type) {
            case OR, AND:
                var computedCondition = buildComposedCondition(condition.type);
                for (var innerCondition : ((ComposedConditionDto) condition).composed) {
                    var innerComputedCondition = buildSecurityMetaCondition(innerCondition);
                    computedCondition.composed.add(innerComputedCondition);
                }

                yield computedCondition;
            case METADATA:
                yield buildSecurityMetaCondition((MetadataConditionDto) condition);
            default:
                throw new BusinessException(ErrorCode.TECHNICAL, "Type " + condition.type + " is not supported");
        };
    }

    private ComposedConditionDto buildComposedCondition(ConditionType type) {
        return switch (type) {
            case AND -> new AndConditionDto();
            case OR -> new OrConditionDto();
            default -> throw new BusinessException(ErrorCode.TECHNICAL, "Type is not a composed condition " + type);
        };
    }

    private ConditionDto buildSecurityMetaCondition(MetadataConditionDto condition) {
        // If metacondition operator is NOT_EQUALS then we must not filter data if metadata is not set on item
        return switch (condition.getOperator()) {
            case EQUALS:
                yield condition;
            case NOT_EQUALS:
                var updatedCondition = new AndConditionDto();
                var existsCondition = new MetadataConditionDto(condition.getMetadata(), null, Operator.EXISTS);
                updatedCondition.composed.add(condition);
                updatedCondition.composed.add(existsCondition);
                yield updatedCondition;
            default:
                throw new BusinessException(ErrorCode.TECHNICAL,
                        "Operator %s is not supported".formatted(condition.getOperator()));
        };
    }

    private OrConditionDto getDatasetsCondition(MonoClassRequestDto request) {
        // Execute the request and map result
        var classId = request.getoClass();
        log.debugf("Get dataset conditions for class %s", classId);
        Collection<DatasetVersionDetailsDto> datasetVersions = new ArrayList<>();
        if (request.getDatasetVersionIds() == null) {
            datasetVersions = datasetVersionService.getAllActiveForClass(classId);
            log.infof("Use Dataset versions (%s):%s ", datasetVersions.size(),
                    datasetVersions.stream().map(DatasetVersionDetailsDto::getId).toList());
        } else {
            log.infof("Use only provided dataset versions %s", request.getDatasetVersionIds());
            for (UUID datasetVersionId : request.getDatasetVersionIds()) {
                var datasetVersionDto = datasetVersionService.get(datasetVersionId);
                if (!datasetVersionDto.getoClass().equals(classId)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "Dataset version and class mismatch " + classId + "<->" + datasetVersionDto.getId());
                }
                datasetVersions.add(datasetVersionDto);
            }
        }
        var datasetCondition = new OrConditionDto();
        datasetVersions.forEach(datasetVersion -> datasetCondition.composed
                .add(new MetadataConditionDto(MetadataSystem.DATASET_VERSION, datasetVersion.getId().toString(),
                        Operator.EQUALS)));
        return datasetCondition;
    }

    private void updateVisibility(Item item, ConditionDto securityMetaCondition) {
        log.tracef("Updating visibility for %s", item);
        for (AttributeSimpleValue attribute : item.getAttributes(AttributeSimpleValue.class)) {
            boolean isVisible = !conditionsEvaluator.conditionEvaluator(attribute, securityMetaCondition);
            attribute.setVisible(isVisible);
        }
        for (AttributeMultiValue attributeMultiValue : item.getAttributes(AttributeMultiValue.class)) {
            for (AttributeSimpleValue attribute : attributeMultiValue.getValues()) {
                boolean isVisible = !conditionsEvaluator.conditionEvaluator(attribute, securityMetaCondition);
                attribute.setVisible(isVisible);
            }
        }
    }

    private void checkNotMixedAttributeClass(OClassDetailsDto classDto, MonoClassRequestDto requestDto) {
        if (requestDto.getCondition() != null) {
            checkNotMixedAttributeClass(classDto, requestDto.getCondition(), requestDto.getCondition().type);
        }
    }

    private void checkNotMixedAttributeClass(OClassDetailsDto classDto, ConditionDto condition, ConditionType type) {
        switch (type) {
            case TRUE, METADATA -> {
            }
            case OR -> {
                OrConditionDto or = (OrConditionDto) condition;
                or.composed.forEach(conditionDto -> checkNotMixedAttributeClass(classDto, conditionDto, conditionDto.type));
            }
            case AND -> {
                AndConditionDto and = (AndConditionDto) condition;
                and.composed.forEach(conditionDto -> checkNotMixedAttributeClass(classDto, conditionDto, conditionDto.type));
            }
            case ATTRIBUTE -> {
                AttributeConditionDto attribute = (AttributeConditionDto) condition;
                classDto.getAttributes().stream().filter(att -> att.getId().equals(attribute.getAttribute())).findAny()
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.BAD_REQUEST, "Attribute not present in class"));
            }
        }
    }

}