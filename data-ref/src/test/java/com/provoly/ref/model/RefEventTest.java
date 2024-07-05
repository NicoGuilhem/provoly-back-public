package com.provoly.ref.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetState;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.model.*;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.ref.*;
import com.provoly.common.user.Role;
import com.provoly.ref.dataset.DatasetMapper;
import com.provoly.ref.dataset.DatasetRepository;
import com.provoly.ref.datasetversion.DatasetVersionController;
import com.provoly.ref.datasetversion.DatasetVersionService;
import com.provoly.ref.model.field.FieldController;
import com.provoly.ref.user.ProvolyUser;
import com.provoly.ref.user.UserService;
import com.provoly.ref.utils.TestService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class RefEventTest {

    @Inject
    Logger log;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    TestService testService;

    @Inject
    ModelController modelController;
    @Inject
    FieldController fieldController;

    @Inject
    DatasetVersionController datasetVersionController;

    @Inject
    DatasetRepository datasetRepository;
    @Inject
    UserService userService;
    @Inject
    DatasetMapper datasetMapper;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    DatasetVersionService datasetVersionService;

    ConsumerTask<String, RefChangeEvent> task;

    @BeforeEach
    void prepare(TestInfo testInfo) {
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        log.infof("Preparing companion for %s", testInfo.getDisplayName());
        companion.topics().clear();

        companion.registerSerde(RefChangeEvent.class, new ObjectMapperSerde<>(RefChangeEvent.class));

        var consumerBuilder = companion.consume(RefChangeEvent.class)
                .withOffsetReset(OffsetResetStrategy.LATEST);
        task = consumerBuilder.fromTopics(RefChangeEvent.TOPIC_NAME);
        // Start the consumer
        // We need to wait partition assignment before test started
        // To be sure we don't miss firsts messages
        log.info("Waiting for assignment");
        consumerBuilder.waitForAssignment().await().atMost(Duration.ofSeconds(10));
        log.infof("Companion ready. Running %s...", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        task.stop();
    }

    @Test
    public void whenFieldAdded_eventSent() throws InterruptedException {
        //given, when
        var field = testService.createAndSaveField("whenFieldAdded_eventSent", Type.STRING);

        //then
        var result = task.awaitRecords(1);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.FIELD_ADDED);
        var refEvent = then(event).asInstanceOf(type(RefChangeEventFieldAdded.class));
        refEvent.extracting(e -> e.getField().getName()).isEqualTo(field.getName());
        refEvent.extracting(e -> e.getField().getName()).asString().contains("whenFieldAdded_eventSent");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_ITEM_WRITE })
    public void getAllFieldsByClass_duplicateFieldAreReturnedOnce() {
        FieldDto field = testService.createAndSaveField();
        AttributeDefDto attributeDef = testService.createAttributeDto(UUID.randomUUID(),
                "attr1", "firstAttributeWithFieldOfNameField1", field);
        AttributeDefDto attributeDto2 = testService.createAttributeDto(UUID.randomUUID(),
                "attr2", "secondAttributeWithFieldOfNameField1", field);

        OClassWriteDto oClass = testService.createClassWriteDto(UUID.randomUUID(), "addTwoAttributesToClass",
                attributeDef, attributeDto2);
        modelController.saveClass(oClass);

        var result = fieldController.getFieldsForClass(oClass.getId());
        assertThat(result).hasSize(1);
    }

    /**
     * When we create a class two events are sent. A field creation followed by a class creation
     */
    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE })
    public void whenClassAdded_eventSent() {

        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr1", "whenClassAdded_eventSent_attr1",
                field);
        OClassWriteDto oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenClassAdded_eventSent_myClass",
                attribute);

        modelController.saveClass(oClass);

        var result = task.awaitRecords(2);

        var event = result.getLastRecord().value();

        then(event.getType()).isEqualTo(RefChangeEvent.Type.CLASS_CREATED);

        var eventAssert = then(event).asInstanceOf(type(RefChangeEventClassCreated.class));
        eventAssert.extracting(e -> e.getoClassDetails().getName()).isEqualTo(oClass.getName());
        var attributeAssert = eventAssert
                .extracting(e -> e.getoClassDetails().getAttributes()).asList()
                .singleElement(type(AttributeDefDetailsDto.class));
        attributeAssert.extracting(AttributeDefDetailsDto::getName).isEqualTo(attribute.getName());
        attributeAssert.extracting(AttributeDefDetailsDto::getField).isNotNull();
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE })
    public void whenClassModified_eventSent() {

        // Given, when
        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr1", "whenClassModified_eventSent_attr1",
                field);
        var oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenClassModified_eventSent_myClass", attribute);
        modelController.saveClass(oClass);
        var attribute2 = testService.createAttributeDto(UUID.randomUUID(), "attr2", "whenClassModified_eventSent_attr2",
                field);
        oClass.getAttributes().add(attribute2);
        modelController.saveClass(oClass);

        //then
        var result = task.awaitRecords(3);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.CLASS_UPDATED);

        var refEvent = then(event).asInstanceOf(type(RefChangeEventClassUpdated.class));
        refEvent.extracting(e -> e.getoClassDetails().getName()).isEqualTo(oClass.getName());
        refEvent.extracting(e -> e.getoClassDetails().getAttributes()).asList().hasSize(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE })
    public void whenClassAddedAttribute_eventSent() {

        //Given
        // Creating class
        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr1", "whenClassAddedAttribute_eventSent_attr1",
                field);
        var oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenClassAddedAttribute_eventSent_myClass", attribute);
        modelController.saveClass(oClass);
        // Modifying class - Add an attribute
        var attribute2 = testService.createAttributeDto(UUID.randomUUID(), "attr2", "whenClassAddedAttribute_eventSent_attr2",
                field);
        modelController.addAttribute(oClass.getId(), attribute2);
        //then
        var result = task.awaitRecords(3);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.CLASS_UPDATED);
        var refEvent = then(event).asInstanceOf(type(RefChangeEventClassUpdated.class));
        refEvent.extracting(e -> e.getoClassDetails().getName()).isEqualTo(oClass.getName());
        refEvent.extracting(e -> e.getoClassDetails().getAttributes()).asList().hasSize(2);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE })
    public void whenClassDeleted_eventSent() {

        //Given
        // Creating class
        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr1", "whenClassDeleted_eventSent_attr1",
                field);
        var oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenClassDeleted_eventSent_myClass", attribute);

        modelController.saveClass(oClass);

        //When
        // Deleting class
        modelController.deleteClass(oClass.getId());

        //Then
        var result = task.awaitRecords(3);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.CLASS_DELETED);
        var refEvent = then(event).asInstanceOf(type(RefChangeEventClassDeleted.class));
        refEvent.extracting(e -> e.getoClassDetails().getName()).isEqualTo(oClass.getName());
        refEvent.extracting(e -> e.getoClassDetails().getAttributes()).asList().hasSize(1);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE,
            Role.STR_DATASET_READ })
    public void whenDataSetActivated_eventSent() {

        // Given
        // Creating class, dataset and dataset version
        testService.authenticate("iamsuperadmin", currentSubjectProvider);
        ProvolyUser provolyUser = userService.getCurrentUser();
        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr3", "whenDataSetActivated_eventSent_attr1",
                field);
        var oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenDataSetActivated_eventSent_myClass", attribute);
        modelController.saveClass(oClass);
        var datasetDto = new DatasetDto(UUID.randomUUID(), "stations activated", oClass.getId(), DatasetType.CLOSED);
        var dataset = datasetMapper.toModel(datasetDto);
        dataset.setUser(provolyUser);
        datasetRepository.save(dataset);
        var datasetVersion = new DatasetVersionDto(UUID.randomUUID(), dataset.getId(), "author", Instant.now());
        datasetVersionController.create(datasetVersion);

        // When
        // Activating dataset
        datasetVersionController.activate(datasetVersion.getId());

        // Then
        var result = task.awaitRecords(3);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.DATASET_VERSION_ACTIVATED);

        var refEvent = then(event).asInstanceOf(type(RefChangeEventDatasetVersionActivated.class));
        refEvent.extracting(RefChangeEventDatasetVersionActivated::getDatasetId).isEqualTo(dataset.getId());
        refEvent.extracting(RefChangeEventDatasetVersionActivated::getDatasetVersionId).isEqualTo(datasetVersion.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_CLASS_WRITE, Role.STR_ITEM_WRITE, Role.STR_DATASET_WRITE,
            Role.STR_DATASET_READ })
    public void whenDeleteDataSetVersion_eventSent() throws InterruptedException {
        // Given
        // Creating class, dataset and dataset version
        var field = testService.createAndSaveField();
        var attribute = testService.createAttributeDto(UUID.randomUUID(), "attr3", "whenDataSetActivated_eventSent_attr1",
                field);
        var oClass = testService.createClassWriteDto(UUID.randomUUID(), "whenDataSetActivated_eventSent_myClass", attribute);
        modelController.saveClass(oClass);
        var dataset = datasetMapper.toModel(new DatasetDto(UUID.randomUUID(), "stations", oClass.getId(), DatasetType.CLOSED));
        dataset.setUser(userService.getCurrentUser());
        datasetRepository.save(dataset);
        UUID datasetVersionId = UUID.randomUUID();
        var datasetVersion = new DatasetVersionDto(datasetVersionId, dataset.getId(), "author", Instant.now());
        datasetVersionController.create(datasetVersion);
        datasetVersionService.changeStateDatasetVersion(datasetVersionId, DatasetState.ACTIVE);
        // When
        // Deleting dataset
        datasetVersionController.delete(datasetVersion.getId());

        // Then
        var result = task.awaitRecords(3);
        var event = result.getLastRecord().value();
        then(event.getType()).isEqualTo(RefChangeEvent.Type.DATASET_VERSION_DELETED);

        var refEvent = then(event).asInstanceOf(type(RefChangeEventDatasetVersionDeleted.class));
        refEvent.extracting(RefChangeEventDatasetVersionDeleted::getoClassDetailsDto)
                .extracting(OClassDetailsDto::getId)
                .isEqualTo(oClass.getId());
        refEvent.extracting(RefChangeEventDatasetVersionDeleted::getDatasetVersionDto)
                .extracting(DatasetVersionDto::getId)
                .isEqualTo(datasetVersion.getId());
    }

}
