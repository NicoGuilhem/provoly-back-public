package com.provoly.virt.search.mono;

import java.util.*;
import java.util.stream.Collectors;

import com.provoly.common.metadata.UserProfileValueReadDto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This user is used is the context of evaluating predicate
 */
@RegisterForReflection
public class User {

    private final String name;
    private final Set<String> roles;
    private final Map<String, List<UserProfileValueReadDto>> metadata;

    User(String name, Set<String> roles, Collection<UserProfileValueReadDto> metadata) {
        this.name = name;
        this.roles = roles;
        this.metadata = metadata.stream().collect(Collectors.groupingBy(userProfile -> userProfile.getUserProfile().name));
    }

    public String getLogin() {
        return name;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public List<String> metadata(String name) {
        var metadata = this.metadata.get(name);
        return metadata == null ? new ArrayList<>() : metadata.stream().map(UserProfileValueReadDto::getValue).toList();
    }

}
