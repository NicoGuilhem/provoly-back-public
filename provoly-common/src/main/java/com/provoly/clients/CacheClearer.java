package com.provoly.clients;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Singleton;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;

import org.jboss.logging.Logger;

@Singleton
public class CacheClearer {

    private final Logger log;
    private final CacheManager cacheManager;

    public CacheClearer(Logger log, CacheManager cacheManager) {
        this.log = log;
        this.cacheManager = cacheManager;
    }

    public void invalidateOClassCaches(UUID id) {
        invalidateCacheWithKey("class-dto-details", id);
        invalidateCacheWithKey("class-dto", id);
        invalidateDefaultCache("model-all-classes");
    }

    private void invalidateCacheWithKey(String cacheName, Object key) {
        Optional<Cache> cache = cacheManager.getCache(cacheName);
        cache.ifPresent(c -> c.invalidate(key)
                .subscribe()
                .with(v -> log.infof("Cache %s is invalidated for key %s", cacheName, key)));
    }

    private void invalidateDefaultCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName).orElseThrow();
        cache.invalidateAll()
                .subscribe()
                .with(v -> log.infof("Cache %s is invalidated", cacheName));
    }
}
