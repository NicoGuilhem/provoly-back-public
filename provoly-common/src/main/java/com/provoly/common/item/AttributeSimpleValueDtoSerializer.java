package com.provoly.common.item;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class AttributeSimpleValueDtoSerializer extends StdSerializer<AttributeSimpleValueDto> {

    protected AttributeSimpleValueDtoSerializer() {
        super(AttributeSimpleValueDto.class);
    }

    @Override
    public void serialize(AttributeSimpleValueDto value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (!value.visible) {
            gen.writeBooleanField("visible", false);
        } else {
            gen.writeObjectField("value", value.value);
        }
        // We never write type for simple value
        gen.writeEndObject();
    }
}
