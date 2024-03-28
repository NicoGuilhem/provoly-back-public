package com.provoly.virt.entity;

import java.util.Objects;
import java.util.UUID;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataDefDto;

public class MetadataValueDto {

    private final MetadataDefDto metadataDef;
    private Object value;

    public MetadataValueDto(MetadataDefDto metadataDef, Object value) {
        this.metadataDef = metadataDef;
        this.value = value;
    }

    public MetadataValueDto(MetadataDefDto metadataDef, String value) {
        this.metadataDef = metadataDef;
        this.value = extractValueFromString(metadataDef, value);
    }

    public String getName() {
        return metadataDef.name;
    }

    public Object getValue() {
        return value;
    }

    public UUID getMetadataId() {
        return metadataDef.id;
    }

    public MetadataDefDto getDefinition() {
        return metadataDef;
    }

    public boolean isEqual(Object otherValue) {
        // FIXME : Better type management
        String currentValue = value.toString();
        return Objects.equals(currentValue, otherValue);
    }

    private Object extractValueFromString(MetadataDefDto metadataDef, String value) {
        switch (metadataDef.type) {
            case INTEGER:
                return Long.parseLong(value);
            case DOUBLE:
                return Double.parseDouble(value);
            case STRING:
            case LIST:
            case UUID:
            case DATE:
                return value;
            default:
                throw new BusinessException(ErrorCode.TECHNICAL, "Unknown metadata type " + metadataDef.type);

        }
    }

}
