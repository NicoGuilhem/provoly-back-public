package com.provoly.ref.metadata;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityId;
import com.provoly.ref.entity.EntityType;

@Entity
public class MetadataValue extends EntityId {

    private String value;
    private UUID entityId;
    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    private UUID metadataDefId;

    protected MetadataValue() {
    }

    public MetadataValue(EntityType type, UUID entityId, UUID metadataDefId) {
        super(UUID.randomUUID());
        this.entityType = type;
        this.entityId = entityId;
        this.metadataDefId = metadataDefId;
    }

    public String getValue() {
        return value;
    }

    public <T extends MetadataAllowedValue> void validateAndSetValue(String value, VariableType type, Set<T> allowedValues) {
        try {
            switch (type) {
                case LIST -> checkValueAllowed(value, allowedValues);
                case INTEGER -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case DATE -> LocalDate.parse(value);
                case UUID -> UUID.fromString(value);
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new IllegalArgumentException("Metadata value should be of type : %s".formatted(type), e);
        }
        this.value = value;
    }

    public <T extends MetadataAllowedValue> void checkValueAllowed(String valueToSet, Set<T> allowedValues) {
        allowedValues.stream()
                .map(MetadataAllowedValue::getValue)
                .filter(value -> value.equals(valueToSet))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        "Values %s isn't allowed.".formatted(valueToSet)));
    }

    public void setType(EntityType type) {
        this.entityType = type;
    }

    public EntityType getType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getMetadataDefId() {
        return metadataDefId;
    }

    public void setMetadataDefId(UUID metadataDefId) {
        this.metadataDefId = metadataDefId;
    }
}
