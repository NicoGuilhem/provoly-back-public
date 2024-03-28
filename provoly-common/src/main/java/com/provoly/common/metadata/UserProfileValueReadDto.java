package com.provoly.common.metadata;

public class UserProfileValueReadDto {
    private String value;

    private UserProfileDto userProfile;

    public UserProfileValueReadDto(String value, UserProfileDto userProfileDto) {
        this.value = value;
        this.userProfile = userProfileDto;
    }

    public UserProfileDto getUserProfile() {
        return userProfile;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUserProfile(UserProfileDto userProfile) {
        this.userProfile = userProfile;
    }

    public String toString() {
        return String.format("name: %s - value: %s", userProfile.name, value);
    }
}
