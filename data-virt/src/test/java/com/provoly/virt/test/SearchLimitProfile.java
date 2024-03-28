package com.provoly.virt.test;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SearchLimitProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("provoly.virt.search-limit", "2");
    }
}
