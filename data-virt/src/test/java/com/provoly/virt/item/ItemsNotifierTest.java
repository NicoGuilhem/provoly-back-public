package com.provoly.virt.item;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.Storage;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.test.ProvolyKafkaCompanionResource;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.virt.entity.Item;
import com.provoly.virt.entity.ItemId;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
@QuarkusTestResource(ProvolyKafkaCompanionResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemsNotifierTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    ItemsNotifier itemsNotifier;

    @Test
    void notifyItemWrittenToStorage() {
        //GIVEN
        AttributeDefDetailsDto attributeDef = Mockito.mock(AttributeDefDetailsDto.class);
        Mockito.when(attributeDef.getTechnicalName()).thenReturn("name");
        List<AttributeDefDetailsDto> attributes = List.of(attributeDef);
        OClassDetailsDto oClassDto = new OClassDetailsDto(UUID.randomUUID(), "slug", "", "", attributes, Storage.ELASTIC,
                List.of());
        ConsumerTask<String, String> consume = companion.consume(String.class, String.class)
                .withGroupId("test")
                .withAutoCommit()
                .fromTopics("class-" + oClassDto.getSlug(), 1);

        ItemId itemId = new ItemId(UUID.randomUUID(), UUID.randomUUID().toString());
        var itemToSend = new Item(itemId, oClassDto);
        itemToSend.getAttributeSimple("name").setValue("fakeName");

        //WHEN
        itemsNotifier.notifyItemWrittenToStorage(itemToSend);
        ConsumerRecord<String, String> firstRecord = consume.awaitCompletion().getFirstRecord();

        //THEN
        Assertions.assertThat(firstRecord.key()).isEqualTo(itemId.getAsString());
        Assertions.assertThat(firstRecord.value()).contains("\"id\":\"" + itemId.getAsString() + "\"");
        Assertions.assertThat(firstRecord.value())
                .contains("\"attributes\":{\"name\":{\"value\":\"fakeName\",\"type\":\"VALUE\"}}");
    }
}