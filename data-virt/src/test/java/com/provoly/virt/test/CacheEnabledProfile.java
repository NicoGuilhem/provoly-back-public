package com.provoly.virt.test;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CacheEnabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.cache.enabled", "true");
    }
}
