package com.provoly.virt.imports.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.clients.AbacService;
import com.provoly.clients.DatasetService;
import com.provoly.clients.DatasetVersionService;
import com.provoly.clients.ModelService;
import com.provoly.common.Storage;
import com.provoly.common.dataset.DatasetDetailsDto;
import com.provoly.common.dataset.DatasetDto;
import com.provoly.common.dataset.DatasetType;
import com.provoly.common.dataset.DatasetVersionDetailsDto;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.ref.RefChangeEventClassCreated;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.Item;
import com.provoly.virt.event.RefChangeListener;
import com.provoly.virt.search.SearchService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AsyncImportServiceTest {
    @Inject
    AsyncImportService asyncImportService;

    @Inject
    AuthService authService;

    @Inject
    SearchService searchService;

    @Inject
    RefChangeListener refChangeListener;

    @InjectMock
    @RestClient
    ModelService modelService;

    @InjectMock
    @RestClient
    DatasetService datasetService;

    @InjectMock
    @RestClient
    DatasetVersionService datasetVersionService;

    @InjectMock
    @RestClient
    AbacService abacService;

    private final UUID classId = UUID.randomUUID();
    private final UUID datasetVersionId = UUID.randomUUID();
    private final FieldDto field = new FieldDto(classId, "field", "string", "slug");
    private final OClassDetailsDto classDetails = new OClassDetailsDto(classId,
            "slug_test_async_import_service",
            "oclass",
            null,
            List.of(new AttributeDefDetailsDto(
                    UUID.randomUUID(),
                    new AttributeDefDto(UUID.randomUUID(), "name", "technical_name", field, null, false, "slug_name"),
                    field, null),
                    new AttributeDefDetailsDto(
                            UUID.randomUUID(),
                            new AttributeDefDto(UUID.randomUUID(), "family_name", "technical_family_name", field, null, false,
                                    "slug_family_name"),
                            field, null)),
            Storage.ELASTIC,
            List.of());

    private final DatasetDto dataset = new DatasetDto(UUID.randomUUID(), "dataset", classId, DatasetType.MODIFIABLE);
    private final DatasetDetailsDto datasetDetail = new DatasetDetailsDto(UUID.randomUUID(), "dataset", classId,
            DatasetType.MODIFIABLE, null, null, null, false, null, null);
    private final DatasetVersionDetailsDto datasetVersionDetail = new DatasetVersionDetailsDto(datasetVersionId, datasetDetail,
            classId, null, null, null, null, null, null, null, null);

    public final String topicDataset = "dataset-%s".formatted(classDetails.getSlug());

    @BeforeEach
    public void init() {
        authService.authenticate();
        when(modelService.getDetails(classId)).thenReturn(classDetails);
        when(datasetService.searchByDatasetVersionId(datasetVersionId)).thenReturn(dataset);

        when(datasetVersionService.getAllActiveForClass(classId)).thenReturn(List.of(datasetVersionDetail));
        when(abacService.getRuleFor(classId)).thenReturn(List.of());

    }

    @Test
    @Order(1) // to be sure that the index is created before the test (used in the second test)
    public void items_created_and_updated_when_records_are_received_in_class_topic() {
        // given
        refChangeListener.refEvent(new RefChangeEventClassCreated(classDetails)); // will create es index with a good mapping

        var firstItemID = UUID.randomUUID();
        var secondItemID = UUID.randomUUID();
        var recordInsertItem1 = generateRecord(firstItemID, null, "technical_name", "Mymi", "technical_family_name", "Cool");
        var recordInsertItem2 = generateRecord(secondItemID, null, "technical_name", "Dam", "technical_family_name", "Super");

        //when
        asyncImportService.consume(recordInsertItem1);
        asyncImportService.consume(recordInsertItem2);
        List<Item> items = searchService.search(new MonoClassRequestDto(classId, null), null).getItems();
        assertThat(items).hasSize(2);
        var firstItem = items.stream().filter(i -> i.getId().getId().equals(firstItemID.toString())).findFirst().get();
        assertThat(firstItem.getAttributes())
                .hasSize(2)
                .containsKeys("technical_name", "technical_family_name")
                .matches(attributeValues -> ((AttributeSimpleValue) attributeValues.get("technical_name")).getValue()
                        .equals("Mymi")
                        && ((AttributeSimpleValue) attributeValues.get("technical_family_name")).getValue().equals("Cool"));
        var secondItem = items.stream().filter(i -> i.getId().getId().equals(secondItemID.toString())).findFirst().get();
        assertThat(secondItem.getAttributes())
                .hasSize(2)
                .containsKeys("technical_name", "technical_family_name")
                .matches(attributeValues -> ((AttributeSimpleValue) attributeValues.get("technical_name")).getValue()
                        .equals("Dam")
                        && ((AttributeSimpleValue) attributeValues.get("technical_family_name")).getValue().equals("Super"));

        // updating items
        var recordReplaceItem1 = generateRecord(firstItemID, ItemUpdateMode.REPLACE, "technical_name", "Myra", null, null);
        var recordUpdateItem2 = generateRecord(secondItemID, ItemUpdateMode.MERGE, "technical_name", "Daminou", null, null);
        asyncImportService.consume(recordReplaceItem1);
        asyncImportService.consume(recordUpdateItem2);

        //then
        items = searchService.search(new MonoClassRequestDto(classId, null), null).getItems();
        assertThat(items).hasSize(2);
        firstItem = items.stream().filter(i -> i.getId().getId().equals(firstItemID.toString())).findFirst().get();
        assertThat(firstItem.getAttributes())
                .hasSize(1)
                .containsKey("technical_name")
                .extractingByKey("technical_name")
                .matches(attributeValue -> ((AttributeSimpleValue) attributeValue).getValue().equals("Myra"));
        secondItem = items.stream().filter(i -> i.getId().getId().equals(secondItemID.toString())).findFirst().get();
        assertThat(secondItem.getAttributes())
                .hasSize(2)
                .containsKeys("technical_name", "technical_family_name")
                .matches(attributeValues -> ((AttributeSimpleValue) attributeValues.get("technical_name")).getValue()
                        .equals("Daminou")
                        && ((AttributeSimpleValue) attributeValues.get("technical_family_name")).getValue().equals("Super"));
    }

    @Test
    public void throw_error_when_header_is_missing() {
        // given
        JsonObject value = new JsonObject();
        value.put("technical_name", "toto");
        var recordMessage = new ConsumerRecord<String, JsonObject>(topicDataset, 0, 0, null, value);

        //when
        assertThatThrownBy(() -> asyncImportService.consume(recordMessage))
                .hasMessage("Missing mandatory header provoly-dataset-version-id");
    }

    @Test
    public void throw_error_when_dataset_is_closed() {
        // given
        DatasetDto invalidDataset = new DatasetDto(UUID.randomUUID(), "invalid-dataset", classId, DatasetType.CLOSED);
        when(datasetService.searchByDatasetVersionId(datasetVersionId)).thenReturn(invalidDataset);
        var recordMessage = generateRecord("technical_name", "toto", "technical_family_name", "toto");

        //when
        assertThatThrownBy(() -> asyncImportService.consume(recordMessage))
                .hasMessageContaining("can't be closed");
    }

    @Test
    public void throw_error_when_invalid_attributes() {
        // given
        var recordMessage = generateRecord("invalid", "titi", "invalid2", "tata");

        //when
        assertThatThrownBy(() -> asyncImportService.consume(recordMessage))
                .hasMessageContaining("Error during attributes validation");
    }

    @Test
    @Order(2) // to be sure that the index is created before the test (done by the first test)
    public void import_does_not_fail_with_attribute_value_null() {
        // given
        var recordMessage = generateRecord("technical_name", "toto", "technical_family_name", null);

        //when
        assertThatNoException().isThrownBy(() -> asyncImportService.consume(recordMessage));
    }

    private ConsumerRecord<String, JsonObject> generateRecord(String attributeName, String attributeValue,
            String attributeName2, String attributeValue2) {
        return generateRecord(null, null, attributeName, attributeValue, attributeName2, attributeValue2);
    }

    private ConsumerRecord<String, JsonObject> generateRecord(UUID itemID, ItemUpdateMode updateMode,
            String attributeName, String attributeValue,
            String attributeName2, String attributeValue2) {
        JsonObject value = new JsonObject();
        value.put(attributeName, attributeValue);
        if (attributeName2 != null) {
            value.put(attributeName2, attributeValue2);
        }
        var recordMessage = new ConsumerRecord(topicDataset, 0, 0, null, value);
        recordMessage.headers().add(
                new RecordHeader("provoly-dataset-version-id", datasetVersionId.toString().getBytes(StandardCharsets.UTF_8)));
        if (itemID != null) {
            recordMessage.headers()
                    .add(new RecordHeader("provoly-item-id", itemID.toString().getBytes(StandardCharsets.UTF_8)));
        }
        if (updateMode != null) {
            recordMessage.headers()
                    .add(new RecordHeader("provoly-item-update-mode", updateMode.name().getBytes(StandardCharsets.UTF_8)));
        }
        return recordMessage;
    }

}
