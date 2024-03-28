package com.provoly.ref.message;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    NOTIFICATION;

    @JsonValue
    public String toLowerCase() {
        return toString().toLowerCase();
    }
}