package com.provoly.virt;

import java.io.IOException;

import com.provoly.common.item.GeoFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class GeoHolderSerializer extends StdSerializer<GeoHolder> {

    protected GeoHolderSerializer() {
        super(GeoHolder.class);
    }

    @Override
    public void serialize(GeoHolder value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var geoFormat = (GeoFormat) provider.getAttribute("GEO_FORMAT");
        String geoValue = value.getStringAs(geoFormat);
        if (geoFormat == GeoFormat.GEO_JSON) {
            gen.writeRawValue(geoValue);

        } else {
            gen.writeString(geoValue);
        }
    }
}
