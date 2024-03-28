package com.provoly.common.transfo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "code", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransfoNodeErrorNoInput.class, name = "NO_INPUT"),
        @JsonSubTypes.Type(value = TransfoNodeErrorMissingAttribute.class, name = "MISSING_PROPERTY_IN_INPUT_ATTRIBUTE"),
        @JsonSubTypes.Type(value = TransfoNodeErrorMissingProperty.class, name = "MISSING_PROPERTY"),
        @JsonSubTypes.Type(value = TransfoNodeErrorBadType.class, name = "BAD_ATTRIBUTE_TYPE"),
        @JsonSubTypes.Type(value = TransfoErrorDatasetConflict.class, name = "DATASET_CONFLICT")
})
public class TransfoNodeError {

}
