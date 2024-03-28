package com.provoly.virt.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.FieldDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.restassured.RestAssured;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
public class ItemsRelationsServiceTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testDataService;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    ItemsController controller;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private RelationTypeDto relationType;

    private AttributeDefDto attribute;

    private FieldDto field;

    @BeforeEach
    public void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        authService.authenticate();
        relationType = testDataService.createRelationTypeDto();

        field = testDataService.createField("name", "keyword");
        attribute = testDataService.createAttribute("name", field);

    }

    @AfterEach
    public void cleaning() {
        testDataService.clean();
    }

    @Test
    void shouldReturnAllRelationsUsingSameDataset() {
        var voitureClass = testDataService.createClass(companion, "voiture", attribute);
        var voitureDs = testDataService.createDataset("voiture", voitureClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "toto");
        var voiture1 = itemsTestTools.addItem(voitureDs, attributes);
        var voiture2 = itemsTestTools.addItem(voitureDs, attributes);
        var voiture3 = itemsTestTools.addItem(voitureDs, attributes);

        itemsTestTools.createRelation(relationType, voiture1, voiture2);
        itemsTestTools.createRelation(relationType, voiture3, voiture1);

        ItemId itemId = new ItemId(voiture1.getId());
        controller.get(itemId);
        ItemsSearchResultDto result = controller.searchWithRelations(itemId);

        assertThat(result.relations()).hasSize(2);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(voitureClass.getId()))
                .extracting("id")
                .containsExactlyInAnyOrder(voiture1.getId(), voiture2.getId(), voiture3.getId());
    }

    @Test
    void shouldReturnAllRelationsUsingDifferentDataset() {
        var voitureClass = testDataService.createClass(companion, "voiture", attribute);
        var voitureDs = testDataService.createDataset("voiture", voitureClass.getId());

        var otherAttribute = testDataService.createAttribute("name", field);
        var usagerClass = testDataService.createClass(companion, "usager", otherAttribute);
        var usagerDs = testDataService.createDataset("usager", usagerClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "toto");
        var voiture1 = itemsTestTools.addItem(voitureDs, attributes);
        var usager1 = itemsTestTools.addItem(usagerDs, attributes);

        itemsTestTools.createRelation(relationType, usager1, voiture1);

        ItemId itemId = new ItemId(voiture1.getId());
        ItemsSearchResultDto result = controller.searchWithRelations(itemId);

        assertThat(result.relations()).hasSize(1);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(voitureClass.getId())).extracting("id").containsExactly(voiture1.getId());
        assertThat(result.items().get(usagerClass.getId())).extracting("id").containsExactly(usager1.getId());
    }
}
