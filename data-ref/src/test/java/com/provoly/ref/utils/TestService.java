package com.provoly.ref.utils;

import static org.mockito.BDDMockito.given;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.Storage;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.model.Type;
import com.provoly.common.model.field.FieldDto;
import com.provoly.ref.KeycloakClientBuilder;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.groups.GroupWrite;
import com.provoly.ref.message.websocket.MessageSocketServer;
import com.provoly.ref.message.websocket.SessionMock;
import com.provoly.ref.model.field.FieldService;
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
    KeycloakClientBuilder keycloakClientBuilder;
    @Inject
    MessageSocketServer messageSocketServer;
    @Inject
    SessionMock sessionMock;
    @Inject
    EntityManager entityManager;
    @Inject
    GroupService groupService;
    @Inject
    GroupRepository groupRepository;
    @Inject
    FieldService fieldService;

    public OClassWriteDto createClassWriteDto(UUID id, String name, AttributeDefDto... attributeDefDtos) {
        return createClassWriteDto(id, name, Storage.ELASTIC, attributeDefDtos);
    }

    public OClassWriteDto createClassWriteDto(UUID id, String name, Storage storage, AttributeDefDto... attributeDefDtos) {
        return new OClassWriteDto(id, name + "-" + id, new ArrayList<>(Arrays.asList(attributeDefDtos)), storage);
    }

    public void ensureGroups(List<String> groupsName) {

        var databaseGroupsNames = groupRepository.getGroupByNames(groupsName).stream().map(EntityNamed::getName).toList();
        groupsName.stream()
                .filter(groupName -> !databaseGroupsNames.contains(groupName))
                .forEach(groupName -> groupService.addGroup(new GroupWrite(UUID.randomUUID(), groupName)));

    }

    public AttributeDefDto createAttributeDto(UUID id, String name, String technicalName, FieldDto fieldDto) {
        return new AttributeDefDto(id, name, technicalName, fieldDto);
    }

    public FieldDto createAndSaveField(UUID fieldId) {
        return createAndSaveField("field1", (Type.STRING).getName().toUpperCase(), fieldId);
    }

    public FieldDto createAndSaveField() {
        return createAndSaveField("field1", Type.STRING);
    }

    public FieldDto createAndSaveField(String name, Type type) {
        return createAndSaveField(name, type.getName(), UUID.randomUUID());
    }

    public FieldDto createAndSaveField(String name, String type, UUID fieldId) {
        var fieldDto = new FieldDto(fieldId, name + "-" + fieldId, type, "");
        fieldService.addField(fieldDto);
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
        entityManager.createNativeQuery(
                "TRUNCATE provoly_user_named_query, named_query, abac_rule, predicate, link, metadata_value, " +
                        "dataset_version_message, dataset_version, category_relations, category, dataset, relation_type, " +
                        "group_relations, dashboard_datasource, dashboard, widget_catalog_datasource, " +
                        "widget_catalog, mono_class_search_request,  condition_condition,  condition, attribute_def, custom_class, oclass, field_condition, field")
                .executeUpdate();
    }
}
