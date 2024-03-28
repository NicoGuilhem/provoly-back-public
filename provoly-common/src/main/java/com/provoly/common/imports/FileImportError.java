package com.provoly.common.imports;

import com.fasterxml.jackson.annotation.JsonInclude;

public record FileImportError(ExtractMessageCode code,
        @JsonInclude(JsonInclude.Include.NON_NULL) FileImportDto.ParamsTypeError params) {

}