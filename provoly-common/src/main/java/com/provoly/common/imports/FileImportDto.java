package com.provoly.common.imports;

import java.util.List;
import java.util.Map;

import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

public record FileImportDto(Integer numberOfLines, Map<Long, List<FileImportError>> itemErrors) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParamsTypeError {
        public static final int RECEIVED_VALUE_MAX_SIZE = 50;
        String name;
        Type type;
        String receivedValue;

        @JsonCreator
        public ParamsTypeError(String name, Type type, String receivedValue) {
            this.name = name;
            this.type = type;
            this.receivedValue = receivedValue == null ? null
                    : receivedValue.substring(0, Math.min(RECEIVED_VALUE_MAX_SIZE, receivedValue.length()));
        }

        public ParamsTypeError(String name) {
            this(name, null, null);
        }

        public String getReceivedValue() {
            return receivedValue;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}