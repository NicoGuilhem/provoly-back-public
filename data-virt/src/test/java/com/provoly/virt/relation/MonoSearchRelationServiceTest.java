package com.provoly.virt.relation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.relation.RelationDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.MetadataConditionDto;
import com.provoly.common.search.Operator;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.test.ItemsTestTools;
import com.provoly.virt.test.SearchResultAssert;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MonoSearchRelationServiceTest {
    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private RelationTypeDto relationType;

    public void prepareData() {
        authService.authenticate();
        relationType = testData.createRelationTypeDto();
    }

    public void cleaning() {
        testData.clean();
    }

    @Test
    @Order(1)
    public void monoQueryResultContainsRelationBetweenItems() {
        prepareData();
        var field = testData.createField("nom_du_field_%s".formatted(UUID.randomUUID()), "keyword");
        var attribute = testData.createAttribute("name", field);

        var voitureClass = testData.createClass(companion, "voiture", attribute);
        var voitureDs = testData.createDataset("voiture", voitureClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "toto");

        var voiture1 = itemsTestTools.addItem(voitureDs, attributes);
        var voiture2 = itemsTestTools.addItem(voitureDs, attributes);

        itemsTestTools.createRelation(relationType, voiture1, voiture2);

        var resultWithRelationsItems = itemsTestTools.searchAllWithRelationsItems(voitureClass.getId(), voitureDs, null);

        assertThat(resultWithRelationsItems.relations())
                .extracting(RelationDto::getRelationType, RelationDto::getSource, RelationDto::getDestination)
                .containsExactly(Tuple.tuple(relationType.slug, voiture1.getId(), voiture2.getId()));

        SearchResultAssert.assertThat(resultWithRelationsItems)
                .hasSizeSourceItemsForClass(voitureClass.getId(), 1)
                .haveSourceItemsForClass(voitureClass.getId(), voiture1)
                .hasSizeDestinationItemsForClass(voitureClass.getId(), 1)
                .haveDestinationItemsForClass(voitureClass.getId(), voiture2);
    }

    @Test
    @Order(2)
    public void relationItemsAreNotRetrievedIfNotRequested() {

        var field = testData.createField("nom_du_field_%s".formatted(UUID.randomUUID()), "keyword");
        var attribute = testData.createAttribute("name", field);

        var voitureClass = testData.createClass(companion, "voiture", attribute);
        var voitureDs = testData.createDataset("voiture", voitureClass.getId());

        var resultWithoutRelationsItems = itemsTestTools.searchAll(voitureClass.getId(), voitureDs);

        assertThat(resultWithoutRelationsItems.sourceItems()).isNullOrEmpty();
        assertThat(resultWithoutRelationsItems.destinationItems()).isNullOrEmpty();
    }

    @Test
    @Order(3)
    public void onlyRelationInItemsOfResultSet() {

        var field1 = testData.createField("field1_%s".formatted(UUID.randomUUID()), "string");
        var attribute = testData.createAttribute("attr1", field1);

        var voitureClass = testData.createClass(companion, "voiture", attribute);
        var voitureDs = testData.createDataset("voiture", voitureClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("attr1", "in");
        var voiture1 = itemsTestTools.addItem(voitureDs, attributes);
        var voiture2 = itemsTestTools.addItem(voitureDs, attributes);

        attributes.put("attr1", "out");
        var voiture3 = itemsTestTools.addItem(voitureDs, attributes);

        itemsTestTools.createRelation(relationType, voiture1, voiture2);

        // A relation between an item of result set and an item out of result set
        // this relation must be returned as filter condition is not applied on relation
        itemsTestTools.createRelation(relationType, voiture1, voiture3);

        var condition = new AttributeConditionDto(attribute.getId(), "in", Operator.EQUALS);
        var result = itemsTestTools.searchAll(voitureClass.getId(), voitureDs, condition);

        assertThat(result.relations())
                .extracting(RelationDto::getRelationType, RelationDto::getSource, RelationDto::getDestination)
                .containsExactlyInAnyOrder(
                        Tuple.tuple(relationType.slug, voiture1.getId(), voiture2.getId()),
                        Tuple.tuple(relationType.slug, voiture1.getId(), voiture3.getId()));

        // searching items filtering withRelation source voiture1
        var resultsForRelationSourceVoiture1 = itemsTestTools.searchAllFilteringOnRelation(voitureClass.getId(), voitureDs,
                new RelationDto(relationType.slug, voiture1.getId(), null));

        SearchResultAssert.assertThat(resultsForRelationSourceVoiture1)
                .hasSizeItemsForClass(voitureClass.getId(), 2)
                .haveItemsForClass(voitureClass.getId(), voiture2, voiture3);

        // searching items filtering withRelation destination voiture3
        var resultsForRelationDestinationVoiture3 = itemsTestTools.searchAllFilteringOnRelation(voitureClass.getId(), voitureDs,
                new RelationDto(relationType.slug, null, voiture3.getId()));

        SearchResultAssert.assertThat(resultsForRelationDestinationVoiture3)
                .hasSizeItemsForClass(voitureClass.getId(), 1)
                .haveItemsForClass(voitureClass.getId(), voiture1);

        // checking items with relation source to voiture1 and additional filter
        MetadataConditionDto voiture3itemIdFilter = new MetadataConditionDto(MetadataSystem.ID, voiture3.getId(),
                Operator.EQUALS);
        var resultsForRelationSourceVoiture1AndAdditionalFilter = itemsTestTools.searchAllFilteringOnRelation(
                voitureClass.getId(), voitureDs,
                new RelationDto(relationType.slug, voiture1.getId(), null), voiture3itemIdFilter);

        SearchResultAssert.assertThat(resultsForRelationSourceVoiture1AndAdditionalFilter)
                .hasSizeItemsForClass(voitureClass.getId(), 1)
                .haveItemsForClass(voitureClass.getId(), voiture3);

        // searching items filtering withRelation source voiture2
        var resultsForRelationSourceVoiture2 = itemsTestTools.searchAllFilteringOnRelation(voitureClass.getId(), voitureDs,
                new RelationDto(relationType.slug, voiture2.getId(), null));

        assertThat(resultsForRelationSourceVoiture2.items()).hasSize(0);

        cleaning();
    }
}
