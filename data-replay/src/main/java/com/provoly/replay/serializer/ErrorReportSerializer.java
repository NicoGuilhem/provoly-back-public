package com.provoly.replay.serializer;

import com.provoly.replay.entity.ErrorReport;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorReportSerializer implements Serializer<ErrorReport> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, ErrorReport data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing ErrorReport to byte[]");
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, ErrorReport data) {
        return Serializer.super.serialize(topic, headers, data);
    }
}
