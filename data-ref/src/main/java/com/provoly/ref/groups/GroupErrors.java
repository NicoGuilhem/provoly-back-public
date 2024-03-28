package com.provoly.ref.groups;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record GroupErrors(Map<UUID, Set<String>> missingGroupsByEntity) {
    public GroupErrors() {
        this(new HashMap<>());
    }
}
