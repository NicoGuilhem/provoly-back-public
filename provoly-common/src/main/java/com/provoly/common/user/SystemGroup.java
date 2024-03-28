package com.provoly.common.user;

import java.util.UUID;
import java.util.function.Predicate;

public enum SystemGroup {

    AUTHENTICATED(UUID.fromString("c9448698-ed9c-4416-b85b-24b8f9fed118")),
    ALL(UUID.fromString("a72c0c63-c871-4515-848f-4a15e5c1f8a6"));

    private UUID id;

    SystemGroup(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public Predicate<UUID> is() {
        return uuid -> id.equals(uuid);
    }
}
