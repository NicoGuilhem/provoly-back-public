package com.provoly.ref.utils;

import static org.mockito.BDDMockito.given;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.ref.KeycloakClientBuilder;
import com.provoly.ref.abac.AbacService;
import com.provoly.ref.abac.predicate.PredicateService;
import com.provoly.ref.dashboard.DashboardService;
import com.provoly.ref.dataset.DatasetService;
import com.provoly.ref.datasetversion.DatasetVersionMessageService;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.entity.EntityType;
import com.provoly.ref.groups.Group;
import com.provoly.ref.groups.GroupRelations;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.groups.GroupWrite;
import com.provoly.ref.link.LinkService;
import com.provoly.ref.message.websocket.MessageSocketServer;
import com.provoly.ref.message.websocket.SessionMock;
import com.provoly.ref.metadata.MetadataService;
import com.provoly.ref.metadata.MetadataValue;
import com.provoly.ref.model.ModelService;
import com.provoly.ref.relation.RelationTypeService;
import com.provoly.ref.user.NamedQueryService;
import com.provoly.ref.widget.WidgetService;
import com.provoly.security.CurrentSubjectProvider;

@ApplicationScoped
public class TestService {

    //    Il n'est pas possible d'importer provoly-common-test-dans data-ref.
    //
    //    provoly-common-test possède le AuthService qui reimplémente le SecurityIdentityAssociation pour "simuler" un access token à data-ref
    //    Data-ref utilise l'annotation @TestSecurity qui provient également du SecurityIdentityAssociation
    //
    //    Il n'est pas possible non plus d'utiliser le AuthService a la place du @TestSecurity car le AuthService appelle le client DataRef.

    @Inject
    ModelService modelService;

    @Inject
    KeycloakClientBuilder keycloakClientBuilder;

    @Inject
    MessageSocketServer messageSocketServer;

    @Inject
    SessionMock sessionMock;
    @Inject
    NamedQueryService namedQueryService;
    @Inject
    DatasetVersionService datasetVersionService;
    @Inject
    EntityManager entityManager;
    @Inject
    DatasetService datasetService;
    @Inject
    MetadataService metadataService;
    @Inject
    DashboardService dashboardService;
    @Inject
    WidgetService widgetService;
    @Inject
    GroupService groupService;
    @Inject
    DatasetVersionMessageService datasetVersionMessageService;
    @Inject
    AbacService abacService;
    @Inject
    PredicateService predicateService;
    @Inject
    LinkService linkService;
    @Inject
    RelationTypeService relationTypeService;

    public OClassWriteDto createClassWriteDto(UUID id, String name, AttributeDefDto... attributeDefDtos) {
        return createClassWriteDto(id, name, Storage.ELASTIC, attributeDefDtos);
    }

    public OClassWriteDto createClassWriteDto(UUID id, String name, Storage storage, AttributeDefDto... attributeDefDtos) {
        return new OClassWriteDto(id, name + "-" + id, new ArrayList<>(Arrays.asList(attributeDefDtos)), storage);
    }

    public void ensureGroups(List<String> groupsName) {

        var databaseGroupsNames = groupService.getGroupByNames(groupsName).stream().map(EntityNamed::getName).toList();
        groupsName.stream()
                .filter(groupName -> !databaseGroupsNames.contains(groupName))
                .forEach(groupName -> groupService.addGroup(new GroupWrite(UUID.randomUUID(), groupName)));

    }

    public AttributeDefDto createAttributeDto(UUID id, String name, UUID fieldId) {
        return createAttributeDto(id, name, name, fieldId);
    }

    public AttributeDefDto createAttributeDto(UUID id, String name, String technicalName, UUID fieldId) {
        AttributeDefDto attributeDefDto = new AttributeDefDto();
        attributeDefDto.id = id;
        attributeDefDto.name = name;
        attributeDefDto.technicalName = technicalName;
        attributeDefDto.field = fieldId;
        return attributeDefDto;
    }

    public void createAndSaveField(UUID fieldId) {
        createAndSaveField("field1", (Type.STRING).getName(), fieldId);
    }

    public FieldDto createAndSaveField() {
        return createAndSaveField("field1", Type.STRING);
    }

    public FieldDto createAndSaveField(String name, Type type) {
        return createAndSaveField(name, type.getName(), UUID.randomUUID());
    }

    public FieldDto createAndSaveField(String name, String type, UUID fieldId) {
        var fieldDto = new FieldDto();
        fieldDto.id = fieldId;
        fieldDto.name = name + "-" + fieldDto.id;
        fieldDto.type = type;
        modelService.addFields(Collections.singleton(fieldDto));
        return fieldDto;
    }

    public void authenticate(String username, CurrentSubjectProvider currentSubjectProvider) {
        given(currentSubjectProvider.getSub()).willReturn(username);
        if (username.equals("anonymous")) {
            given(currentSubjectProvider.getGroups()).willReturn(Set.of("ALL"));
            given(currentSubjectProvider.getName()).willReturn("anonymous");
            return;
        }

        try (var keycloak = keycloakClientBuilder.build()) {
            var users = keycloak.realm("provoly").users().search(username);
            if (users.size() != 1) {
                throw new IllegalArgumentException("Unable to retrieve keycloak admin user id " + users);
            }

            String iamuseradminSub = users.getFirst().getId();
            given(currentSubjectProvider.getSub()).willReturn(iamuseradminSub);

            if (username.equals("iamsuperadmin")) {
                given(currentSubjectProvider.getGroups()).willReturn(Set.of("ALL", "AUTHENTICATED"));
                given(currentSubjectProvider.getName()).willReturn("iamsuperadmin");
            } else if (username.equals("iampolice")) {
                given(currentSubjectProvider.getGroups()).willReturn(Set.of("ALL", "AUTHENTICATED"));
                given(currentSubjectProvider.getName()).willReturn("iampolice");

            }
            messageSocketServer.onOpen(sessionMock);
        }
    }

    public void authenticate(CurrentSubjectProvider currentSubjectProvider) {
        authenticate("iamsuperadmin", currentSubjectProvider);
    }

    @Transactional
    public void clean() {
        namedQueryService.getNamedQueriesForCurrentUser()
                .forEach(nq -> namedQueryService.removeNamedQueryIfExists(nq.getNamedQuery().getId()));
        abacService.getAllRules().forEach(abac -> entityManager.remove(entityManager.merge(abac)));
        predicateService.getAllPredicates().forEach(predicate -> entityManager.remove(entityManager.merge(predicate)));
        linkService.getAll().forEach(link -> linkService.delete(link.getId()));
        datasetVersionService.getAll().forEach(dv -> {
            List<MetadataValue> metadataValues = metadataService.getMetadataValueByEntityId(dv.getId());
            metadataValues.forEach(metadataValue -> {
                metadataService.deleteMetadataValueByEntityId(dv.getId(), metadataValue.getMetadataDefId(),
                        EntityType.DATASET_VERSION);
            });
            datasetVersionMessageService.deleteAllDatasetVersionMessage(dv.getId());
            entityManager.remove(dv);
        });
        datasetService.getAll().forEach(dataset -> {
            List<MetadataValue> metadataValues = metadataService.getMetadataValueByEntityId(dataset.getId());
            metadataValues.forEach(metadataValue -> {
                metadataService.deleteMetadataValueByEntityId(dataset.getId(),
                        metadataValue.getMetadataDefId(), EntityType.DATASET);
            });
            entityManager.remove(entityManager.merge(dataset));
        });
        relationTypeService.getAll().forEach(relationType -> relationTypeService.delete(relationType.getId()));
        dashboardService.getAll().forEach(dashboard -> {
            List<Group> groups = groupService.getGroupsByEntityId(dashboard.getId());
            groups.forEach(r -> {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaDelete<GroupRelations> delete = cb.createCriteriaDelete(GroupRelations.class);
                Root<GroupRelations> e = delete.from(GroupRelations.class);
                delete.where(cb.equal(e.get("entityId"), r.getId()));
                entityManager.createQuery(delete).executeUpdate();
            });
            dashboardService.delete(dashboard.getId());
        });
        widgetService.getAll().forEach(widgetCatalog -> entityManager.remove(widgetCatalog));
        namedQueryService.getNamedQueriesForCurrentUser().forEach(nq -> entityManager.remove(nq));
    }
}
