package com.provoly.virt.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.common.item.ItemDto;
import com.provoly.common.kafka.KafkaTools;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public void notifyItemWritedToElastic(Map<String, List<ItemDto>> itemsBySlug) {
        var itemsCount = new HashMap<String, AtomicLong>();
        try {
            for (Map.Entry<String, List<ItemDto>> entry : itemsBySlug.entrySet()) {
                String topicName = buildTopicName(entry.getKey());
                for (var item : entry.getValue()) {
                    String itemJson = mapper.writeValueAsString(item);
                    kafkaTools.createTopicIfNeeded(topicName);
                    var record = new ProducerRecord<>(topicName, item.getId(), itemJson);
                    producer.send(record);
                    itemsCount.computeIfAbsent(topicName, k -> new AtomicLong(0)).addAndGet(1);
                }
                log.debugf("Items sent to kafka: %s", itemsCount);
            }

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String buildTopicName(String slug) {
        return "class-" + slug;
    }

}
