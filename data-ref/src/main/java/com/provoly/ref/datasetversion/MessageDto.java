package com.provoly.ref.datasetversion;

import java.util.List;

import com.provoly.common.imports.MessageLevel;

public record MessageDto(MessageLevel level, List<DatasetVersionMessageDto> messages, Long count) {
}
