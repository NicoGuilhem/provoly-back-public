package com.provoly.ref.datasetversion;

import java.util.UUID;

import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.MessageLevel;
import com.provoly.common.model.Type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "level", visible = true)
public record DatasetVersionMessageDto(MessageLevel level, ExtractMessageCode extractMessageCode, Type type, String name,
        String recordId, UUID datasetVersionId) {
}
