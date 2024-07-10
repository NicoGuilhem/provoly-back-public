package com.provoly.ref.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.VariableType;
import com.provoly.common.abac.AbacRuleDto;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.link.LinkDto;
import com.provoly.common.metadata.MetadataDefDto;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.AttributeDefWriteDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.search.*;
import com.provoly.common.user.Role;
import com.provoly.ref.abac.AbacMapper;
import com.provoly.ref.abac.AbacService;
import com.provoly.ref.abac.predicate.Predicate;
import com.provoly.ref.abac.predicate.PredicateService;
import com.provoly.ref.customclass.CustomClassService;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.link.LinkMapper;
import com.provoly.ref.link.LinkService;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.metadata.MetadataMapper;
import com.provoly.ref.relation.RelationTypeService;
import com.provoly.ref.searchrequest.AttributeCondition;
import com.provoly.ref.user.NamedQueryController;
import com.provoly.ref.user.NamedQueryService;
import com.provoly.ref.user.VisibilityType;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ModelServiceTest {
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    ModelController modelController;
    @Inject
    TestService testService;
    @Inject
    ModelService modelService;
    @Inject
    ModelMapper modelMapper;
    @Inject
    LinkService linkService;
    @Inject
    LinkMapper linkMapper;
    @Inject
    RelationTypeService relationTypeService;
    @Inject
    NamedQueryService namedQueryService;
    @Inject
    Logger logger;
    @Inject
    NamedQueryController namedQueryController;
    @Inject
    PredicateService predicateService;
    @Inject
    AbacService abacService;
    @Inject
    AbacMapper abacMapper;
    @Inject
    AssociationService associationService;
    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetadataMapper metadataMapper;
    @Inject
    CustomClassService customClassService;
    @Inject
    DatasetService datasetService;

    private final UUID nqPrivateId = UUID.randomUUID();
    private final UUID attributeId = UUID.randomUUID();
    private final UUID abacId = UUID.randomUUID();
    private OClassWriteDto classDto = null;
    private AttributeDefDto attributeDefDto = new AttributeDefDto();
    private MetadataDefDto metadataDefDto = null;
    private FieldDto fieldDto = null;

    @BeforeEach
    public void init() {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        fieldDto = testService.createAndSaveField();
        attributeDefDto = testService.createAttributeDto(attributeId, "attributeName", "attributeId" + attributeId,
                fieldDto);
        classDto = testService.createClassWriteDto(UUID.randomUUID(), "classDto", attributeDefDto);
        modelService.saveEntity(modelMapper.toModel(classDto));
        metadataDefDto = createMetadataDef();
    }

    @AfterEach
    @Transactional
    public void clear() {
        testService.clean();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_WRITE, Role.STR_LINK_WRITE })
    public void delete_attributeReferencedByLink_throwError400() {
        UUID relationTypeUUID = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        relationTypeService.saveOrUpdate(new RelationTypeDto(relationTypeUUID, "Appartient à"));
        var link = new LinkDto();
        link.id = linkId;
        link.relationType = relationTypeUUID;
        link.attributeDestination = attributeId;
        link.attributeSource = attributeId;
        linkService.save(linkMapper.toModel(link));

        assertThatThrownBy(() -> modelController.deleteAttribute(classDto.getId(), attributeId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "The attribute %s is used by one or more search(es) and abac rule(s), remove them to delete the attribute"
                                .formatted(attributeId));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH, Role.STR_CLASS_WRITE })
    public void delete_inexistantAttribute_throwError403() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> modelController.deleteAttribute(classDto.getId(), id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "AttributeDef : %s inexistant.".formatted(id));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void delete_unusedAttribute_return204() {
        modelService.deleteAttributeById(classDto.getId(), attributeId);
        var result = modelService.getOClassById(classDto.getId());
        assertThat(result.getAttributes()).isEmpty();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ })
    public void get_usageOfUnusedAttribute() {
        var result = associationService.getAttributeAssociations(attributeId);
        assertThat(result.associations()).isNullOrEmpty();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfAttributeReferencedByNamedquery() {
        NamedQueryDto namedquery = createNamedQueryDto(nqPrivateId, "namedqueryPublic", VisibilityType.PUBLIC, attributeId,
                classDto);
        namedQueryController.saveNamedQuery(namedquery);

        var result = associationService.getAttributeAssociations(attributeId);
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(nqPrivateId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfAttributeReferencedByComposedNamedquery() {
        NamedQueryDto namedquery = createComposedNamedQueryDto(nqPrivateId, "namedqueryPublic", VisibilityType.PUBLIC,
                attributeId,
                classDto);
        namedQueryController.saveNamedQuery(namedquery);

        var result = associationService.getAttributeAssociations(attributeId);
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(nqPrivateId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfAttributeReferencedByAbac() {
        createRuleCondition(attributeDefDto, attributeId);
        var result = associationService.getAttributeAssociations(attributeId);
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(abacId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfAttributeReferencedByLink() {
        UUID relationTypeUUID = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        relationTypeService.saveOrUpdate(new RelationTypeDto(relationTypeUUID, "Appartient à"));
        var link = new LinkDto();
        link.id = linkId;
        link.relationType = relationTypeUUID;
        link.attributeDestination = attributeId;
        link.attributeSource = attributeId;
        linkService.save(linkMapper.toModel(link));
        var result = associationService.getAttributeAssociations(attributeId);
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(linkId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfClassReferencedByLink() {
        UUID relationTypeUUID = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        relationTypeService.saveOrUpdate(new RelationTypeDto(relationTypeUUID, "Appartient à"));
        var link = new LinkDto();
        link.id = linkId;
        link.relationType = relationTypeUUID;
        link.attributeDestination = attributeId;
        link.attributeSource = attributeId;
        linkService.save(linkMapper.toModel(link));
        var result = associationService.getClassAssociations(classDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(linkId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfClassReferencedByNamedquery() {
        NamedQueryDto namedquery = createNamedQueryDto(nqPrivateId, "namedqueryPublic", VisibilityType.PUBLIC, attributeId,
                classDto);
        namedQueryController.saveNamedQuery(namedquery);

        var result = associationService.getClassAssociations(classDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(nqPrivateId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfClassReferencedByComposedNamedquery() {
        NamedQueryDto namedquery = createComposedNamedQueryDto(nqPrivateId, "namedqueryPublic", VisibilityType.PUBLIC,
                attributeId,
                classDto);
        namedQueryController.saveNamedQuery(namedquery);

        var result = associationService.getClassAssociations(classDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(nqPrivateId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfClassReferencedByAbac() {
        createRuleCondition(attributeDefDto, attributeId);
        var result = associationService.getClassAssociations(classDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(abacId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_READ, Role.STR_ITEM_WRITE, Role.STR_FIELD_READ,
            Role.STR_FIELD_WRITE, Role.STR_SEARCH })
    public void get_usageOfClassReferencedByMultiClassSearchRequest() {
        NamedQueryDto namedquery = createMultiNamedQueryDto(nqPrivateId, "namedqueryPublic", VisibilityType.PUBLIC, classDto);
        namedQueryController.saveNamedQuery(namedquery);
        var result = associationService.getClassAssociations(classDto.getId());
        assertThat(result.associations())
                .extracting(AssociationDto::getId)
                .isEqualTo(List.of(nqPrivateId))
                .hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE })
    public void get_listStorages() {
        var result = modelService.getStorages();
        assertThat(result)
                .containsExactlyInAnyOrder(Storage.POSTGIS, Storage.ELASTIC)
                .hasSize(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASOURCE_READ })
    public void addMetadataToClass_shouldSucceed() {
        MetadataValueWriteDto metadataValueWriteDto = new MetadataValueWriteDto();
        metadataValueWriteDto.setMetadataDefId(metadataDefDto.id);
        metadataValueWriteDto.setValue("12");

        modelController.setMetadataToClass(classDto.getId(), metadataDefDto.id, metadataValueWriteDto);

        assertThat(modelController.getDetails(classDto.getId()))
                .isInstanceOf(OClassDetailsDto.class)
                .extracting(OClassDetailsDto::getMetadata)
                .extracting(metadata -> metadata.getFirst().getValue())
                .isEqualTo("12");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASOURCE_READ })
    public void deleteMetadataToClass_shouldSucceed() {
        UUID metadataValueWrite = UUID.randomUUID();
        MetadataValueWriteDto metadataValueWriteDto = new MetadataValueWriteDto();
        metadataValueWriteDto.setMetadataDefId(metadataValueWrite);
        metadataValueWriteDto.setValue("12");

        modelController.setMetadataToClass(classDto.getId(), metadataDefDto.id, metadataValueWriteDto);

        modelController.deleteMetadataToClass(classDto.getId(), metadataDefDto.id);

        assertThat(modelController.getDetails(classDto.getId()))
                .isInstanceOf(OClassDetailsDto.class)
                .extracting(OClassDetailsDto::getMetadata)
                .isEqualTo(List.of());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASOURCE_READ })
    public void saveClassWithMetadata_shouldSucceed() {
        UUID attributeId = UUID.randomUUID();
        FieldDto fieldDto = testService.createAndSaveField();
        AttributeDefWriteDto attributeDefDto = testService.createAttributeWriteDto(attributeId, "attribute",
                "attributeId" + attributeId,
                fieldDto);
        MetadataValueWriteDto metadataValueWriteDto = new MetadataValueWriteDto();
        metadataValueWriteDto.setMetadataDefId(metadataDefDto.id);
        metadataValueWriteDto.setValue("12");

        UUID oclassId = UUID.randomUUID();
        OClassWriteDto oclassWriteDto = new OClassWriteDto(oclassId, "oclass1", "icon", List.of(attributeDefDto), "slug",
                Storage.ELASTIC, List.of(metadataValueWriteDto));

        modelController.saveClass(oclassWriteDto);

        assertThat(modelController.getDetails(oclassId))
                .isInstanceOf(OClassDetailsDto.class)
                .extracting(OClassDetailsDto::getMetadata)
                .extracting(x -> x.get(0).getValue())
                .isEqualTo("12");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASOURCE_READ })
    public void deleteClassReferencedByDataset_shouldTrow() {
        UUID attributeId = UUID.randomUUID();
        FieldDto fieldDto = testService.createAndSaveField();
        AttributeDefWriteDto attributeDefDto = testService.createAttributeWriteDto(attributeId, "attribute",
                "attributeId" + attributeId,
                fieldDto);

        UUID oclassId = UUID.randomUUID();
        OClassWriteDto oclassWriteDto = new OClassWriteDto(oclassId, "oclass2", "icon", List.of(attributeDefDto), "slug",
                Storage.ELASTIC, List.of());

        modelController.saveClass(oclassWriteDto);
        datasetService.save(createClosedDataset("dataset", oclassId));

        assertThatThrownBy(() -> modelController.deleteClass(oclassId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "OClass contains one or more abac rules, links, datasets or namedqueries, remove them to delete the oClass %s"
                                .formatted(oclassWriteDto.getName()));
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_CLASS_READ, Role.STR_SEARCH,
            Role.STR_DATASOURCE_READ })
    public void deleteClassWithCustomClass_shouldSucceed() {
        UUID attributeId = UUID.randomUUID();
        FieldDto fieldDto = testService.createAndSaveField();
        AttributeDefWriteDto attributeDefDto = testService.createAttributeWriteDto(attributeId, "attribute",
                "attributeId" + attributeId,
                fieldDto);

        UUID oclassId = UUID.randomUUID();
        OClassWriteDto oclassWriteDto = new OClassWriteDto(oclassId, "oclass3", "icon", List.of(attributeDefDto), "slug",
                Storage.ELASTIC, List.of());

        modelController.saveClass(oclassWriteDto);
        customClassService.addCustomClass(oclassId, "tooltip", "");
        customClassService.addCustomClass(oclassId, "test2", "");
        modelController.deleteClass(oclassId);

        assertThatThrownBy(() -> modelController.getById(oclassId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("%s : %s inexistant.".formatted("OClass", oclassId));
    }

    public DatasetDto createClosedDataset(String name, UUID oclassId) {
        return new DatasetDto(UUID.randomUUID(), name, oclassId, DatasetType.CLOSED, "description");
    }

    private MetadataDefDto createMetadataDef() {
        MetadataDefDto metadataDefDto = new MetadataDefDto();
        metadataDefDto.id = UUID.randomUUID();
        metadataDefDto.name = "metadataDef" + metadataDefDto.id;
        metadataDefDto.description = "description";
        metadataDefDto.type = VariableType.STRING;
        metadataDefDto.slug = "slug";
        metadataDefDto.allowedValues = null;
        metadataDefService.addMetadata(metadataMapper.toModel(metadataDefDto));
        return metadataDefDto;
    }

    private NamedQueryDto createMultiNamedQueryDto(UUID id, String name, VisibilityType visibilityType,
            OClassWriteDto classDto) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name(), List.of());
        return new NamedQueryDto(id, name, "description", new MultiClassRequestDto(List.of(classDto.getId()), List.of()), vis);
    }

    private void createRule(AbacRuleType type, String predicate, UUID attributeId) {
        AbacRuleDto rule = new AbacRuleDto();
        rule.name = "rule";
        rule.id = abacId;
        rule.type = type;
        rule.predicate = createPredicate(predicate).getId();
        rule.condition = new AttributeConditionDto(attributeId, "value", Operator.EXISTS);
        abacService.save(abacMapper.toModel(rule));
    }

    private void createRuleCondition(AttributeDefDto attributeDef, UUID attributeId) {
        var ruleCondition = new AttributeCondition();
        ruleCondition.setAttribute(modelMapper.toModel(attributeDef));
        ruleCondition.setOperator(Operator.EQUALS);
        ruleCondition.setLocation("location");
        ruleCondition.setValue("value");
        ruleCondition.setUpperValue("upperValue");
        createRule(AbacRuleType.ATTRIBUTE, "user.metadata('statut') == 'policier'", attributeId);
    }

    private Predicate createPredicate(String value) {
        Predicate predicate = new Predicate(UUID.randomUUID());
        predicate.setName("name");
        predicate.setValue(value);
        predicateService.save(predicate);
        return predicate;
    }

    private NamedQueryDto createComposedNamedQueryDto(UUID id, String name, VisibilityType visibilityType, UUID attributeId,
            OClassWriteDto classDto) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name(), List.of());
        var attributeCondition1 = new AttributeConditionDto(attributeId, "2", Operator.EQUALS, null);
        var composedCondition = new AndConditionDto();
        composedCondition.composed.add(attributeCondition1);
        composedCondition.composed.add(attributeCondition1);
        return new NamedQueryDto(id, name, "description",
                new MonoClassRequestDto(classDto.getId(), List.of(), composedCondition), vis);
    }

    private NamedQueryDto createNamedQueryDto(UUID id, String name, VisibilityType visibilityType, UUID attributeId,
            OClassWriteDto classDto) {
        VisibilityDto vis = new VisibilityDto(visibilityType.name(), List.of());
        return new NamedQueryDto(id, name, "description", new MonoClassRequestDto(classDto.getId(), List.of(),
                new AttributeConditionDto(attributeId, "2", null, null, Operator.EQUALS)), vis);
    }
}
