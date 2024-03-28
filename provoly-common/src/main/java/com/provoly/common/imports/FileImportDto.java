package com.provoly.common.imports;

import java.util.List;
import java.util.Map;

import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonInclude;

public record FileImportDto(Integer numberOfLines, Map<Long, List<FileImportError>> itemErrors) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record ParamsTypeError(String name, Type type) {

        public ParamsTypeError(String name) {
            this(name, null);
        }
    }
}