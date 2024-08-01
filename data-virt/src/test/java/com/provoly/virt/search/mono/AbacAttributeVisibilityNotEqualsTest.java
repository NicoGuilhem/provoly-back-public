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
public class AbacAttributeVisibilityNotEqualsTest {

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
    private AttributeDefDto authorAttribute;
    private ItemDto document;

    public void prepareData() {
        authService.init();

        // create class document (title is open data, author is sensible data)
        var titleField = testData.createField("titre_du_field_%s".formatted(UUID.randomUUID()), "string");
        var authorField = testData.createField("author_du_field_%s".formatted(UUID.randomUUID()), "string");
        this.titleAttribute = testData.createAttribute("title", titleField);
        this.authorAttribute = testData.createAttribute("author", authorField);
        documentClass = testData.createClass(companion, "document", this.titleAttribute, this.authorAttribute);
        datasetVersionDto = testData.createDataset("ds-document", documentClass.getId());
    }

    private void addRuleOnMetadata(Operator operator) {
        // Create metadata
        var classificationMeta = testData.createMetadataItem("classification");

        // Add a rule on classification SECRET
        testData.createMetadataRule(classificationMeta, operator, "open", "user.metadata('statut').contains('policier')");
        insertItems();

        itemsTestTools.addMetadataToAttribute(document, authorAttribute, classificationMeta, "secret");
    }

    private void addRuleOnAttribute(Operator operator) {
        var ruleCondition = new AttributeConditionDto(authorAttribute.getId(), "toto", operator);

        testData.createAttributeRule(ruleCondition, "user.metadata('statut').contains('policier')");
        insertItems();
    }

    public void cleaning() {
        testData.clean();
    }

    private void insertItems() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Compte rendu audition");
        attributes.put(authorAttribute.getName(), "Jason Bourne");
        document = itemsTestTools.addItem(datasetVersionDto, attributes);
    }

    @Test
    @Order(1)
    public void user_canSeeTitleAndNotAuthor() {
        prepareData();
        addRuleOnMetadata(Operator.NOT_EQUALS);
        authService.authenticate(AuthService.User.USER);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto);
        assertThat(result.items()).hasSize(1);
        var doc = result.items().get(documentClass.getId()).get(0);
        assertThat(doc.getSimpleAttribute("title").visible).isTrue();
        assertThat(doc.getSimpleAttribute("author").visible).isFalse();
    }

    @Test
    @Order(2)
    public void police_canSeeTitleAndAuthor() {
        authService.init();
        addRuleOnMetadata(Operator.NOT_EQUALS);
        authService.authenticate(AuthService.User.POLICE);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto);
        assertThat(result.items()).hasSize(1);
        var doc = result.items().get(documentClass.getId()).get(0);
        assertThat(doc.getSimpleAttribute("title").visible).isTrue();
        assertThat(doc.getSimpleAttribute("author").visible).isTrue();
    }

    @Test
    @Order(3)
    public void police_should_get_one_item_with_filter_not_equals_toto() {
        authService.init();
        addRuleOnAttribute(Operator.NOT_EQUALS);
        authService.authenticate(AuthService.User.POLICE);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @Order(4)
    public void user_should_get_no_item_with_filter_not_equals_toto() {
        authService.init();
        addRuleOnAttribute(Operator.NOT_EQUALS);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto);
        assertThat(result.items()).isEmpty();
        cleaning();
    }

}
