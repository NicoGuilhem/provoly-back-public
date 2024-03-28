package com.provoly.common.item;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/***
 * Class used to serialize ItemsSearchResult
 */
public class ItemsSearchResultDtoSerializer extends StdSerializer<ItemsSearchResultDto> {

    private final JsonSerializer<Object> defaultSerializer;

    public ItemsSearchResultDtoSerializer(JsonSerializer<Object> defaultSerializer) {
        super(ItemsSearchResultDto.class);
        this.defaultSerializer = defaultSerializer;
    }

    @Override
    public void serialize(ItemsSearchResultDto value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        provider.setAttribute("GEO_FORMAT", value.geoFormat());
        defaultSerializer.serialize(value, gen, provider);
    }
}
