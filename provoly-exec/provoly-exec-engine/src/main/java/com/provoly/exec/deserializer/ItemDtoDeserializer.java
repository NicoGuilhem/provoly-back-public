package com.provoly.exec.deserializer;

import com.provoly.common.item.ItemDto;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class ItemDtoDeserializer extends ObjectMapperDeserializer<ItemDto> {
    public ItemDtoDeserializer() {
        super(ItemDto.class);
    }
}