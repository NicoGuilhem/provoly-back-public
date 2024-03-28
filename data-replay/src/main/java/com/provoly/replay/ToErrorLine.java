package com.provoly.replay;

import java.nio.charset.StandardCharsets;

import com.provoly.replay.entity.ErrorLine;

import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;

public class ToErrorLine implements ValueTransformerWithKey<String, String, ErrorLine> {
    ProcessorContext context;

    @Override
    public void init(ProcessorContext processorContext) {
        this.context = processorContext;
    }

    @Override
    public ErrorLine transform(String key, String value) {
        boolean resetString = false;
        String excString = "";

        var reset = context.headers().lastHeader("reset");
        if (reset != null) {
            resetString = Boolean.parseBoolean(new String(reset.value(), StandardCharsets.UTF_8));
        }

        var exc = context.headers().lastHeader("exception");
        if (exc != null) {
            excString = new String(exc.value(), StandardCharsets.UTF_8);
        }
        return new ErrorLine(context.topic(), excString, resetString);
    }

    @Override
    public void close() {

    }
}
