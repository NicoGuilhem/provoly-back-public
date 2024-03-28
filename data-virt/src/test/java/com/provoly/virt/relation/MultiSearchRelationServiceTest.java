package com.provoly.virt.relation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.provoly.common.relation.RelationDto;
import com.provoly.common.relation.RelationTypeDto;
import com.provoly.common.search.FieldConditionDto;
import com.provoly.common.search.MultiClassRequestDto;
import com.provoly.common.search.MultiSearchType;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.test.TestDataService;
import com.provoly.virt.search.SearchController;
import com.provoly.virt.test.ItemsTestTools;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiSearchRelationServiceTest {

    @Inject
    AuthService authService;

    @Inject
    TestDataService testData;

    @Inject
    ItemsTestTools itemsTestTools;

    @Inject
    SearchController searchController;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private RelationTypeDto relationType;

    @BeforeEach
    public void prepareData() {
        authService.authenticate();
        relationType = testData.createRelationTypeDto();
    }

    @AfterEach
    public void cleaning() {
        testData.clean();
    }

    @Test
    public void multiQueryResultContainsRelationBetweenItems() {

        var field1 = testData.createField("field1", "integer");

        var attributeVoiture = testData.createAttribute("attr1", field1);
        var voitureClass = testData.createClass(companion, "voiture", attributeVoiture);
        var voitureDs = testData.createDataset("voiture", voitureClass.getId());

        var attributeCaracteristique = testData.createAttribute("attr1", field1);
        var carateristiqueClass = testData.createClass(companion, "caracteristique", attributeCaracteristique);
        var caracteristiqueDs = testData.createDataset("caracteristique", carateristiqueClass.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("attr1", 42);
        var caracteristique1 = itemsTestTools.addItem(caracteristiqueDs, attributes);
        var voiture1 = itemsTestTools.addItem(voitureDs, attributes);

        attributes.put("attr1", 0);
        var voiture2 = itemsTestTools.addItem(voitureDs, attributes);

        itemsTestTools.createRelation(relationType, voiture1, caracteristique1);
        itemsTestTools.createRelation(relationType, voiture2, caracteristique1);

        FieldConditionDto fieldCondition = new FieldConditionDto(field1.id, "42");
        var request = new MultiClassRequestDto(MultiSearchType.AND, List.of(), List.of(fieldCondition));
        var result = searchController.search(request, null);

        assertThat(result.relations())
                .extracting(RelationDto::getRelationType, RelationDto::getSource, RelationDto::getDestination)
                .containsExactly(Tuple.tuple(relationType.slug, voiture1.getId(), caracteristique1.getId()));
    }

}
