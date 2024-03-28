package com.provoly.transfo.runner;

import com.provoly.common.item.ItemDto;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

public class ItemDtoSerde extends ObjectMapperSerde<ItemDto> {

    public ItemDtoSerde() {
        super(ItemDto.class);
    }
}
