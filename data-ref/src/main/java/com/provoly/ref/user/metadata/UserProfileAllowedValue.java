package com.provoly.ref.user.metadata;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.provoly.ref.metadata.MetadataAllowedValue;

@Entity
public class UserProfileAllowedValue extends MetadataAllowedValue {
    @ManyToOne
    private UserProfile userProfile;

    protected UserProfileAllowedValue() {
        super();
    }

    public UserProfileAllowedValue(String value) {
        super(value);
    }

    public void setMetadataUserDef(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

}
