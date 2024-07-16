package com.provoly.ref.user.metadata;

import java.util.UUID;

import jakarta.persistence.Entity;

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

    public UserProfileValue(UserProfile userProfile, UUID provolyUserId, String value) {
        this(userProfile.getId(), provolyUserId);
        userProfile.getType().checkValue(value,
                userProfile.getValues().stream().map(UserProfileAllowedValue::getValue).toList());
        this.value = value;
    }

    public String getValue() {
        return value;
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
