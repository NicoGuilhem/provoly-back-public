package com.provoly.common.item;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class ItemsSearchResultDtoSerializerModifier extends BeanSerializerModifier {

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
            JsonSerializer<?> serializer) {
        if (beanDesc.getBeanClass() == ItemsSearchResultDto.class) {
            return new ItemsSearchResultDtoSerializer((JsonSerializer<Object>) serializer);
        }
        return super.modifySerializer(config, beanDesc, serializer);
    }
}
