package com.provoly.common.imports;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ExtractedMessage(
        MessageLevel messageLevel,
        ExtractMessageCode code,
        @JsonInclude(JsonInclude.Include.NON_NULL) FileImportDto.ParamsTypeError params) {
    public ExtractedMessage(MessageLevel messageLevel, ExtractMessageCode extractMessageCode) {
        this(messageLevel, extractMessageCode, null);
    }
}