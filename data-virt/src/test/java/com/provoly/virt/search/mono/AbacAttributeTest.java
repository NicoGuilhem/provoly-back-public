package com.provoly.virt.search.mono;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.ConditionDto;
import com.provoly.common.search.FullSearchConditionDto;
import com.provoly.common.search.Operator;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbacAttributeTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private OClassWriteDto documentClass;
    private DatasetVersionDto datasetVersionDto;
    private AttributeDefDto titleAttribute;
    private AttributeDefDto nbWordAttribute;
    private AttributeDefDto authorAttribute;
    private ItemDto sensibleDoc;
    private ItemDto openDoc;
    private ConditionDto condition;

    public void prepareData() {
        authService.init();

        // create class vehicule
        var titleField = testData.createField("title_%s".formatted(UUID.randomUUID()), "string");
        var nbWordField = testData.createField("nbWord_%s".formatted(UUID.randomUUID()), "integer");
        var authorField = testData.createField("author_%s".formatted(UUID.randomUUID()), "string");
        titleAttribute = testData.createAttribute("title", titleField);
        nbWordAttribute = testData.createAttribute("nbWord", nbWordField);
        authorAttribute = testData.createAttribute("author", authorField);
        documentClass = testData.createClass(companion, "document", titleAttribute, nbWordAttribute, authorAttribute);
        datasetVersionDto = testData.createDataset("ds-document", documentClass.getId());

        // Add a rule on POLICE can see author with Dredd
        var ruleCondition = new AttributeConditionDto(authorAttribute.getId(), "dredd", Operator.CONTAINS);
        testData.createAttributeRule(ruleCondition, "user.metadata('statut').contains('policier')");

        condition = new AttributeConditionDto(nbWordAttribute.getId(), "42", Operator.EQUALS);

        insertItems();
    }

    public void cleaning() {
        testData.clean();
    }

    private void insertItems() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Compte rendu audition Jason Bourne");
        attributes.put(nbWordAttribute.getName(), 42);
        attributes.put(authorAttribute.getName(), "Paul Dredd");
        sensibleDoc = itemsTestTools.addItem(datasetVersionDto, attributes);
        attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Liste des schtroumpfs");
        attributes.put(nbWordAttribute.getName(), 42);
        attributes.put(authorAttribute.getName(), "La Schtroumpfette et paul le grand Schtroumpf");
        openDoc = itemsTestTools.addItem(datasetVersionDto, attributes);
        attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Etat de la guerre politique en Balkanie");
        attributes.put(nbWordAttribute.getName(), 56);
        attributes.put(authorAttribute.getName(), "Le grand Jean"); // Attribute secured by meta
        itemsTestTools.addItem(datasetVersionDto, attributes);
    }

    @Test
    @Order(1)
    public void sensibleDoc_user_cannotSee() {
        prepareData();
        authService.authenticate(AuthService.User.USER);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result.items().get(documentClass.getId())).extracting("id").containsExactlyInAnyOrder(openDoc.getId());

    }

    @Test
    @Order(2)
    public void sensibleDoc_police_canSee() {
        authService.authenticate(AuthService.User.POLICE);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result.items().get(documentClass.getId())).extracting("id").containsExactlyInAnyOrder(sensibleDoc.getId(),
                openDoc.getId());
    }

    @Test
    @Order(3)
    public void onAuthorSearch_user_cannotSee_authorSensibleDoc() {
        authService.authenticate(AuthService.User.USER);
        var condition = new AttributeConditionDto(authorAttribute.getId(), "paul", Operator.CONTAINS);

        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result.items().get(documentClass.getId())).extracting("id").containsExactlyInAnyOrder(openDoc.getId());

    }

    @Test
    @Order(4)
    public void onFullSearch_user_cannotSee_authorSensibleDoc() {
        authService.authenticate(AuthService.User.USER);
        var condition = new FullSearchConditionDto("paul");

        var result = itemsTestTools.searchFull(condition);
        assertThat(result.items().get(documentClass.getId())).extracting("id").containsExactlyInAnyOrder(openDoc.getId());
        cleaning();

    }

}
