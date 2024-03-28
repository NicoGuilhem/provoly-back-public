package com.provoly.ref.user.metadata;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityId;

@Entity
public class UserProfileValue extends EntityId {

    private String value;

    private UUID provolyUserId;

    private UUID userProfileId;

    protected UserProfileValue() {
    }

    public UserProfileValue(UUID userProfileId, UUID provolyUserId) {
        super(UUID.randomUUID());
        this.userProfileId = userProfileId;
        this.provolyUserId = provolyUserId;
    }

    public String getValue() {
        return value;
    }

    public <T extends UserProfileAllowedValue> void validateAndSetValue(String value, VariableType type, Set<T> allowedValues) {
        try {
            switch (type) {
                case LIST -> checkValueAllowed(value, allowedValues);
                case INTEGER -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case DATE -> LocalDate.parse(value);
                case UUID -> UUID.fromString(value);
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new IllegalArgumentException("User profile value should be of type : %s".formatted(type), e);
        }
        this.value = value;
    }

    public <T extends UserProfileAllowedValue> void checkValueAllowed(String valueToSet, Set<T> allowedValues) {
        allowedValues.stream()
                .map(UserProfileAllowedValue::getValue)
                .filter(value -> value.equals(valueToSet))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        "Values %s isn't allowed.".formatted(valueToSet)));
    }

    public UUID getProvolyUserId() {
        return provolyUserId;
    }

    public void setProvolyUserId(UUID provolyUserId) {
        this.provolyUserId = provolyUserId;
    }

    public UUID getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(UUID userProfileId) {
        this.userProfileId = userProfileId;
    }
}
