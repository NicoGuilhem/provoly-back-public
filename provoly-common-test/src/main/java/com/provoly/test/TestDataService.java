package com.provoly.test;

import static io.smallrye.reactive.messaging.kafka.companion.RecordQualifiers.until;

import java.util.*;

import jakarta.inject.Singleton;

import com.provoly.clients.*;
import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.abac.PredicateDto;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.common.model.field.FieldDateDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.model.field.FieldGeoDto;
import com.provoly.common.model.field.FieldNumericDto;
import com.provoly.common.ref.RefChangeEvent;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.search.*;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Singleton
public class TestDataService {

    private final Logger log;
    private final ProvolyUserService provolyUserService;
    private final DatasetService datasetService;
    private final DatasetVersionService datasetVersionService;
    private final MetadataRefService metadataRefService;
    private final ModelService modelService;
    private final AbacService abacService;
    private final PredicateService predicateService;
    private final RelationTypeService relationTypeService;
    private final AuthService authService;

    private final Collection<OClassWriteDto> classDtos = new ArrayList<>();
    private final Collection<DatasetDto> datasets = new ArrayList<>();
    private final Collection<RelationTypeDto> relationTypes = new ArrayList<>();
    private final Collection<AbacRuleDto> rules = new ArrayList<>();
    private final Collection<NamedQueryDto> namedQueries = new ArrayList<>();

    public TestDataService(Logger log, @RestClient ProvolyUserService provolyUserService,
            @RestClient DatasetService datasetService,
            @RestClient DatasetVersionService datasetVersionService, @RestClient MetadataRefService metadataRefService,
            @RestClient ModelService modelService, @RestClient AbacService abacService,
            @RestClient PredicateService predicateService,
            @RestClient RelationTypeService relationTypeService, AuthService authService) {
        this.provolyUserService = provolyUserService;
        this.datasetService = datasetService;
        this.datasetVersionService = datasetVersionService;
        this.metadataRefService = metadataRefService;
        this.modelService = modelService;
        this.abacService = abacService;
        this.predicateService = predicateService;
        this.relationTypeService = relationTypeService;
        this.authService = authService;
        log.info("New test data context");
        this.log = log;
    }

    /**
     * Delete all data used by tests
     * WARNING : CurrentSearch and NamedQuery are common as test user are sames for all tests run
     */
    public void clean() {
        log.info("Cleaning all datas");
        // FIXME : We should start a service data-ref in devservices too much thing are in common now : rule, ...
        namedQueries.forEach(nq -> provolyUserService.deleteNamedQuery(nq.getId()));
        namedQueries.clear();
        authService.authenticate(AuthService.User.SUPER_ADMIN);
        deactivateAllRule();
        datasets.forEach(c -> {
            datasetService.getAllById(c.getId()).forEach(dv -> datasetVersionService.delete(dv.getId()));
        });
        datasets.clear();
        rules.forEach(this::deleteRule);
        rules.clear();
        classDtos.clear();
        relationTypes.forEach(rt -> relationTypeService.deleteRelationType(rt.id));
        relationTypes.clear();
    }

    private void deactivateAllRule() {
        // FIXME : Not compatible with multiple test running in parallel
        for (var rule : abacService.getAllRules()) {
            rule.active = false;
            abacService.addRule(rule);
        }
    }

    public DatasetVersionDto createDataset(String name, UUID oclassId) {
        var dataset = new DatasetDto(oclassId, name, oclassId, DatasetType.MODIFIABLE, List.of("ALL"), List.of());
        return saveDataset(dataset);
    }

    public DatasetDto createClosedDataset(String name, UUID oclassId) {
        var dataset = new DatasetDto(UUID.randomUUID(), name, oclassId, DatasetType.CLOSED);
        datasetService.save(dataset);
        datasets.add(dataset);
        return dataset;
    }

    private DatasetVersionDto saveDataset(DatasetDto dataset) {
        datasetService.save(dataset);
        log.infof("Dataset created " + dataset.getName());
        datasets.add(dataset);
        DatasetVersionDetailsDto datasetVersion = datasetService.getDatasetVersionByDatasetId(dataset.getId());
        return new DatasetVersionDto(datasetVersion.getId(), datasetVersion.getDataset().getId(),
                datasetVersion.getoClass(), datasetVersion.getLastModified(), datasetVersion.getVersion(),
                datasetVersion.getState(), datasetVersion.getFileName(), datasetVersion.getProductionDate(),
                datasetVersion.getProducer(), datasetVersion.getAdditionalInformation());
    }

    public FieldDto createField(String name, String type, String additionalProperty) {
        FieldDto fieldDto;
        if (Type.from(type).isGeo()) {
            fieldDto = new FieldGeoDto(UUID.randomUUID(), name, type, type, additionalProperty);
        } else if (Type.from(type).isDate()) {
            fieldDto = new FieldDateDto(UUID.randomUUID(), name, type, type, additionalProperty);
        } else if (Type.from(type).isNumeric()) {
            fieldDto = new FieldNumericDto(UUID.randomUUID(), name, type, type, false, additionalProperty);
        } else {
            fieldDto = new FieldDto(UUID.randomUUID(), name, type, type);
        }
        modelService.addField(fieldDto);
        return fieldDto;
    }

    public FieldDto createField(String name, String type) {
        return createField(name, type, null);
    }

    public FieldDto createField(String name, Type type) {
        return createField(name, type.getName(), null);
    }

    public FieldDto createField(String name, Type type, String additionalProperties) {
        return createField(name, type.getName(), additionalProperties);
    }

    public AttributeDefDto createAttributeMulti(String name, String technicalName, FieldDto field, boolean multi) {
        return new AttributeDefDto(UUID.randomUUID(), name, technicalName, field, null, multi, "");
    }

    public AttributeDefDto createAttributeMulti(String name, FieldDto field, boolean multi) {
        return createAttributeMulti(name, name, field, multi);
    }

    public AttributeDefDto createAttribute(String name, String technicalName, FieldDto field) {
        return new AttributeDefDto(UUID.randomUUID(), name, technicalName, field);
    }

    public AttributeDefDto createAttribute(String name, FieldDto field) {
        return createAttribute(name, name, field);
    }

    public OClassWriteDto createClass(KafkaCompanion companion, String name, AttributeDefDto... attributes) {
        return createClass(companion, name, Storage.ELASTIC, attributes);
    }

    public OClassWriteDto createClass(KafkaCompanion companion, String name, Storage storage,
            AttributeDefDto... attributes) {
        return createClassWithId(companion, UUID.randomUUID(), name, storage, null, attributes);
    }

    public OClassWriteDto createClassWithId(KafkaCompanion companion, UUID id, String name, Storage storage,
            List<MetadataValueWriteDto> metadata,
            AttributeDefDto... attributes) {
        var classDto = new OClassWriteDto(id, name + "-" + id, List.of(attributes), storage, metadata);
        if (storage == Storage.KUZZLE_ASSET || storage == Storage.KUZZLE_MEASURE) {
            modelService.addClass(classDto);
        } else {
            // Give time to the RefEvent consumer to create the index
            waitUntilClassIsReady(companion, () -> modelService.addClass(classDto));
        }
        classDtos.add(classDto);
        return classDto;
    }

    public OClassWriteDto createClassWithoutIdInName(String name, AttributeDefDto... attributes) {
        var classDto = new OClassWriteDto(UUID.randomUUID(), name, List.of(attributes), Storage.ELASTIC);
        modelService.addClass(classDto);
        classDtos.add(classDto);

        return classDto;
    }

    public RelationTypeDto createRelationTypeDto() {
        UUID id = UUID.randomUUID();
        var relationType = new RelationTypeDto(id, "relation-type-" + id.hashCode()); // uuid is too long to be part of name
        relationType.slug = "5000_relation-type";
        relationTypeService.addRelationType(relationType);
        relationTypes.add(relationType);
        return relationType;
    }

    public MetadataDefDto createMetadataItem(String name) {
        MetadataDefDto metadataDef = new MetadataDefDto();
        metadataDef.id = UUID.randomUUID();
        metadataDef.name = name + "-" + metadataDef.id;
        metadataDef.type = VariableType.STRING;
        metadataRefService.saveMetadataDef(metadataDef);
        return metadataDef;
    }

    public PredicateDto createPredicate(String value) {
        PredicateDto predicate = new PredicateDto();
        predicate.id = UUID.randomUUID();
        predicate.name = "name";
        predicate.value = value;
        predicateService.savePredicate(predicate);
        return predicate;
    }

    public void createAttributeRule(ConditionDto condition, String predicate) {
        createRule(AbacRuleType.ATTRIBUTE, condition, predicate);
    }

    public void createMetadataRule(MetadataDefDto metadata, Operator operator, String value, String predicate) {
        createRule(AbacRuleType.METADATA, new MetadataConditionDto(metadata.id, value, operator), predicate);
    }

    public void createMetadataRule(MetadataDefDto metadata, String value, String predicate) {
        createRule(AbacRuleType.METADATA, new MetadataConditionDto(metadata.id, value, Operator.EQUALS), predicate);
    }

    private void createRule(AbacRuleType type, ConditionDto condition, String predicate) {
        var rule = new AbacRuleDto();
        rule.id = UUID.randomUUID();
        rule.name = "rule-" + rule.id;
        rule.predicate = createPredicate(predicate).id;
        rule.active = true;
        rule.type = type;
        rule.condition = condition;
        abacService.addRule(rule);
        rules.add(rule);
    }

    public NamedQueryDto createNamedQuery(String name, SearchRequestDto request) {
        var namedQuery = new NamedQueryDto(UUID.randomUUID(), name, "description", request,
                new VisibilityDto("PRIVATE", List.of()));
        provolyUserService.saveNamedQuery(namedQuery);
        namedQueries.add(namedQuery);
        return namedQuery;
    }

    public void deleteDatasetVersion(UUID datasetVersionId) {
        datasetVersionService.delete(datasetVersionId);
    }

    private void deleteRule(AbacRuleDto r) {
        abacService.deleteRule(r.id);
    }

    private static void waitUntilClassIsReady(KafkaCompanion companion, Runnable runnable) {
        companion.registerSerde(RefChangeEvent.class, new ObjectMapperSerde<>(RefChangeEvent.class));

        ConsumerTask<String, RefChangeEvent> consumerTask = companion.consume(RefChangeEvent.class)
                .withOffsetReset(OffsetResetStrategy.EARLIEST)
                .fromTopics("ref-event", until(event -> event.value().getType().equals(RefChangeEvent.Type.CLASS_READY)));
        runnable.run();
        consumerTask.awaitCompletion();
    }

}