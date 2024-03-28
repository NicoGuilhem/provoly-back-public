package com.provoly.replay.serializer;

import java.util.Map;

import com.provoly.replay.entity.ErrorReport;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorReportDeserializer implements Deserializer<ErrorReport> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public ErrorReport deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(new String(data, "UTF-8"), ErrorReport.class);
        } catch (Exception e) {
            throw new SerializationException("Error when deserializing byte[] to ErrorReport " + e);
        }
    }

    @Override
    public ErrorReport deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
