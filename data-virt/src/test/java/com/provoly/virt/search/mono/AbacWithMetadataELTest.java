package com.provoly.virt.search.mono;

import static com.provoly.virt.test.SearchResultAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.item.ItemDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassWriteDto;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.ConditionDto;
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
public class AbacWithMetadataELTest {

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
    private ItemDto policeDoc;
    private ItemDto openDoc;

    private ConditionDto condition;

    public void prepareData() {
        authService.init();
        authService.authenticate(AuthService.User.SUPER_ADMIN);

        // create class vehicule
        var titleField = testData.createField("title", "string");
        var nbWordField = testData.createField("nbWord", "integer");
        var authorField = testData.createField("author", "string");
        titleAttribute = testData.createAttribute("title", titleField);
        nbWordAttribute = testData.createAttribute("nbWord", nbWordField);
        authorAttribute = testData.createAttribute("author", authorField);
        documentClass = testData.createClass(companion, "document", titleAttribute, nbWordAttribute, authorAttribute);
        datasetVersionDto = testData.createDataset("ds-document", documentClass.getId());

        // Add a rule : can see doc if item statut_juridique is same as user statut
        var statutJuridique = testData.createMetadataItem("statut_juridique");

        insertItems();

        condition = new AttributeConditionDto(nbWordAttribute.getId(), "42", Operator.EQUALS);
        itemsTestTools.addMetadataToItem(policeDoc, statutJuridique, "policier");

        testData.createMetadataRule(statutJuridique, Operator.NOT_EQUALS, "${user.metadata('statut')}",
                "user.login == 'iamsuperadmin'");
    }

    public void cleaning() {
        testData.clean();
    }

    private void insertItems() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Compte rendu audition Jason Bourne");
        attributes.put(nbWordAttribute.getName(), 42);
        attributes.put(authorAttribute.getName(), "Paul Dredd");
        policeDoc = itemsTestTools.addItem(datasetVersionDto, attributes);
        attributes = new HashMap<>();
        attributes.put(titleAttribute.getName(), "Liste des schtroumpfs");
        attributes.put(nbWordAttribute.getName(), 42);
        attributes.put(authorAttribute.getName(), "La Schtroumpfette et le grand Schtroumpf");
        openDoc = itemsTestTools.addItem(datasetVersionDto, attributes);
    }

    @Test
    @Order(1)
    public void admin_cansee_policeDoc() {
        prepareData();
        authService.authenticate(AuthService.User.SUPER_ADMIN);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result).haveItemsForClass(documentClass.getId(), policeDoc, openDoc);
    }

    @Test
    @Order(2)
    public void policier_cansee_policeDoc() {
        authService.authenticate(AuthService.User.POLICE);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result).haveItemsForClass(documentClass.getId(), policeDoc, openDoc);
    }

    @Test
    @Order(3)
    public void lawyer_cannotsee_policeDoc() {
        authService.authenticate(AuthService.User.LAWYER);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result).haveItemsForClass(documentClass.getId(), openDoc);
    }

    /**
     * User have no metadata statut
     */
    @Test
    @Order(4)
    public void user_cannotsee_policeDoc() {
        authService.authenticate(AuthService.User.USER);
        var result = itemsTestTools.searchAll(documentClass.getId(), datasetVersionDto, condition);
        assertThat(result).haveItemsForClass(documentClass.getId(), openDoc);
    }

}
