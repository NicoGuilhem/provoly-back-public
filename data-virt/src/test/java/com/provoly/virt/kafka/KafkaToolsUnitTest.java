package com.provoly.virt.kafka;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.vertx.core.json.JsonObject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;

public class KafkaToolsUnitTest {

    @Test
    void testExtractHeaderValueFromRecord_HeaderPresent() {
        // Arrange
        String headerName = "test-header";
        String headerValue = "header-value";
        ConsumerRecord<String, JsonObject> recordMessage = mock(ConsumerRecord.class);
        Headers headers = mock(Headers.class);
        Header header = mock(Header.class);

        when(recordMessage.headers()).thenReturn(headers);
        when(headers.lastHeader(headerName)).thenReturn(header);
        when(header.value()).thenReturn(headerValue.getBytes(StandardCharsets.UTF_8));

        // Act
        Optional<String> result = KafkaTools.extractHeaderValueFromRecord(recordMessage, headerName);

        // Assert
        assertThat(result).isPresent().hasValue(headerValue);
    }

    @Test
    void testExtractHeaderValueFromRecord_HeaderNotPresent() {
        // Arrange
        String headerName = "test-header";
        ConsumerRecord<String, JsonObject> recordMessage = mock(ConsumerRecord.class);
        Headers headers = mock(Headers.class);

        when(recordMessage.headers()).thenReturn(headers);
        when(headers.lastHeader(headerName)).thenReturn(null);

        // Act
        Optional<String> result = KafkaTools.extractHeaderValueFromRecord(recordMessage, headerName);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void testExtractHeaderValueFromRecord_HeaderValueBlank() {
        // Arrange
        String headerName = "test-header";
        ConsumerRecord<String, JsonObject> recordMessage = mock(ConsumerRecord.class);
        Headers headers = mock(Headers.class);
        Header header = mock(Header.class);

        when(recordMessage.headers()).thenReturn(headers);
        when(headers.lastHeader(headerName)).thenReturn(header);
        when(header.value()).thenReturn("".getBytes(StandardCharsets.UTF_8));

        // Act
        Optional<String> result = KafkaTools.extractHeaderValueFromRecord(recordMessage, headerName);

        // Assert
        assertThat(result).isEmpty();
    }
}
