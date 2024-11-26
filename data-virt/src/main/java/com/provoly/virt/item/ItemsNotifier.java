package com.provoly.virt.item;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.item.ItemDto;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.Item;
import com.provoly.virt.kafka.KafkaTools;
import com.provoly.virt.search.mono.MonoMapper;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@ApplicationScoped
public class ItemsNotifier {

    @Inject
    Logger log;

    // TODO should be a KafkaProducer<UUID, Item> with custom serializers (for key and value)
    @Inject
    KafkaProducer producer;

    @Inject
    KafkaTools kafkaTools;

    @Inject
    ObjectMapper mapper;

    @Inject
    MonoMapper itemMapper;

    public void notifyItemWrittenToStorage(Item item) {
        notifyItemWrittenToStorage(List.of(item));
    }

    public void notifyItemWrittenToStorage(Collection<Item> items) {

        try {
            ObjectWriter itemWriter = mapper.writer().forType(ItemDto.class).withAttribute("GEO_FORMAT", GeoFormat.WKT);
            for (Item item : items) {
                // Items are not read from a user and a security context but received from external system without any authentication
                // We have to unlock visibility
                item.getAttributes(AttributeSimpleValue.class).forEach(attribute -> attribute.setVisible(true));
                var itemDto = itemMapper.toDto(item);
                String itemJson = itemWriter.writeValueAsString(itemDto);
                String topicName = buildTopicName(item.getoClass().getSlug()); // One topic per class
                kafkaTools.createTopicIfNeeded(topicName); // TODO : Retention, policies, etc
                var itemRecord = new ProducerRecord<>(topicName, itemDto.getId(), itemJson);
                log.tracef("Send notification for item %s to topic %s", itemDto.getId(), topicName);
                producer.send(itemRecord);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to notify items written", e);
        }

    }

    private String buildTopicName(String slug) {
        return "class-" + slug;
    }

}
