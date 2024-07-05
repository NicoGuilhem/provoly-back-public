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
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.AttributeDefDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.model.field.FieldDto;
import com.provoly.common.ref.RefChangeEventClassCreated;
import com.provoly.common.search.MonoClassRequestDto;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.virt.event.RefChangeListener;
import com.provoly.virt.search.SearchService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
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
            "slug",
            "oclass",
            null,
            List.of(new AttributeDefDetailsDto(
                    classId,
                    new AttributeDefDto(classId,
                            "name", "name", field, null, false, "slug"),
                    field, null),
                    new AttributeDefDetailsDto(
                            classId,
                            new AttributeDefDto(classId, "family_name", "name", field, null, false, "slug"),
                            field, null)),
            Storage.ELASTIC,
            List.of());

    private final DatasetDto dataset = new DatasetDto(UUID.randomUUID(), "dataset", classId, DatasetType.MODIFIABLE);
    private final DatasetDetailsDto datasetDetail = new DatasetDetailsDto(UUID.randomUUID(), "dataset", classId,
            DatasetType.MODIFIABLE, null, null, null, false, null, null);
    private final DatasetVersionDetailsDto datasetVersionDetail = new DatasetVersionDetailsDto(datasetVersionId, datasetDetail,
            classId, null, null, null, null, null, null, null, null);

    public final String TOPIC_DATASET = "dataset-%s".formatted(classDetails.getSlug());

    @BeforeEach
    public void init() {
        authService.authenticate();
        when(modelService.getDetails(classId)).thenReturn(classDetails);
        when(datasetService.searchByDatasetVersionId(datasetVersionId)).thenReturn(dataset);

        when(datasetVersionService.getAllActiveForClass(classId)).thenReturn(List.of(datasetVersionDetail));
        when(abacService.getRuleFor(classId)).thenReturn(List.of());

    }

    @Test
    public void items_created_when_records_are_received_in_class_topic() {
        // given
        var record1 = generateRecord("name", "toto", "family_name", "toto");
        var record2 = generateRecord("name", "titi", "family_name", "titi");
        refChangeListener.refEvent(new RefChangeEventClassCreated(classDetails)); // will create es index with a good mapping

        //when
        asyncImportService.consume(record1);
        asyncImportService.consume(record2);

        //then
        assertThat(searchService.search(new MonoClassRequestDto(classId, null), null).getItems())
                .hasSize(2);
    }

    @Test
    public void throw_error_when_header_is_missing() {
        // given
        JsonObject value = new JsonObject();
        value.put("name", "toto");
        var record = new ConsumerRecord(TOPIC_DATASET, 0, 0, null, value);

        //when
        assertThatThrownBy(() -> asyncImportService.consume(record))
                .hasMessage("Dataset header is mandatory");
    }

    @Test
    public void throw_error_when_dataset_is_closed() {
        // given
        DatasetDto invalidDataset = new DatasetDto(UUID.randomUUID(), "invalid-dataset", classId, DatasetType.CLOSED);
        when(datasetService.searchByDatasetVersionId(datasetVersionId)).thenReturn(invalidDataset);
        var record = generateRecord("name", "toto", "family_name", "toto");

        //when
        assertThatThrownBy(() -> asyncImportService.consume(record))
                .hasMessageContaining("can't be closed");
    }

    @Test
    public void throw_error_when_invalid_attributes() {
        // given
        var record = generateRecord("invalid", "titi", "invalid2", "tata");

        //when
        assertThatThrownBy(() -> asyncImportService.consume(record))
                .hasMessageContaining("Error during attributes validation");
    }

    @Test
    public void import_does_not_fail_with_attribute_value_null() {
        // given
        JsonObject value = new JsonObject();
        var record = generateRecord("name", "toto", "family_name", null);

        //when
        assertThatNoException().isThrownBy(() -> asyncImportService.consume(record));
    }

    private ConsumerRecord<String, JsonObject> generateRecord(String attributeName, String attributeValue,
            String attributeName2, String attributeValue2) {
        JsonObject value = new JsonObject();
        value.put(attributeName, attributeValue);
        value.put(attributeName2, attributeValue2);
        var record = new ConsumerRecord(TOPIC_DATASET, 0, 0, null, value);
        record.headers().add(
                new RecordHeader("provoly-dataset-version-id", datasetVersionId.toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("provoly-item-id", attributeValue.getBytes(StandardCharsets.UTF_8)));
        return record;
    }

}
