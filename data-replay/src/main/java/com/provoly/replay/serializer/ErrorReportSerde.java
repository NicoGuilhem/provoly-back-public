package com.provoly.replay.serializer;

import java.util.Map;

import com.provoly.replay.entity.ErrorReport;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class ErrorReportSerde implements Serde<ErrorReport> {
    private ErrorReportSerializer serializer = new ErrorReportSerializer();
    private ErrorReportDeserializer deserializer = new ErrorReportDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    public Serializer<ErrorReport> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<ErrorReport> deserializer() {
        return deserializer;
    }
}
