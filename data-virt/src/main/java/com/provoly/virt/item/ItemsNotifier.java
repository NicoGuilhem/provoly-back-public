package com.provoly.virt.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.item.ItemDto;
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
            var itemsCount = new HashMap<String, AtomicLong>();
            ObjectWriter itemWriter = mapper.writer().forType(ItemDto.class).withAttribute("GEO_FORMAT", GeoFormat.WKT);
            for (Item item : items) {
                String topicName = buildTopicName(item.getoClass().getSlug()); // One topic per class
                var itemDto = itemMapper.toDto(item);
                String itemJson = itemWriter.writeValueAsString(itemDto);
                kafkaTools.createTopicIfNeeded(topicName); // TODO : Retention, policies, etc
                var record = new ProducerRecord<>(topicName, itemDto.getId(), itemJson);
                producer.send(record);
                itemsCount.computeIfAbsent(topicName, k -> new AtomicLong(0)).addAndGet(1);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to notify items written", e);
        }

    }

    private String buildTopicName(String slug) {
        return "class-" + slug;
    }

}
