package com.provoly.clients;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Singleton;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;

import org.jboss.logging.Logger;

@Singleton
public class CacheClearer {

    private Logger log;
    private CacheManager cacheManager;

    public CacheClearer(Logger log, CacheManager cacheManager) {
        this.log = log;
        this.cacheManager = cacheManager;
    }

    public void invalidateOClassCaches(UUID id) {
        invalidateCacheWithKey("class-dto-details", id);
        invalidateCacheWithKey("class-dto", id);
    }

    private void invalidateCacheWithKey(String cacheName, Object key) {
        Optional<Cache> cache = cacheManager.getCache(cacheName);
        cache.ifPresent(c -> c.invalidate(key)
                .subscribe()
                .with(v -> log.infof("Cache %s is invalidated", cacheName)));
    }
}
