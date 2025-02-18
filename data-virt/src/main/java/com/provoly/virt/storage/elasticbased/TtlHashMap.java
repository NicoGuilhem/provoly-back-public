package com.provoly.virt.storage.elasticbased;

import static java.util.Collections.unmodifiableSet;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

public class TtlHashMap<K, V> implements Map<K, V> {

    private final Duration ttl;
    private final Map<K, TimestampedValue> store = new ConcurrentHashMap<>();

    public TtlHashMap(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public V get(Object key) {
        var tsValue = this.store.get(key);

        if (tsValue == null) {
            return null;
        } else {
            if (tsValue.expired()) {
                store.remove(key);
                return null;
            } else {
                return tsValue.value;
            }
        }

    }

    @Override
    public V put(K key, V value) {
        clearExpired();
        var previous = store.put(key, new TimestampedValue(value));
        return previous == null ? null : previous.value;
    }

    @Override
    public int size() {
        clearExpired();
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        clearExpired();
        return store.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        clearExpired();
        return store.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        clearExpired();
        return store.containsValue(value);
    }

    @Override
    public V remove(Object key) {
        var tsValue = store.remove(key);
        return tsValue == null ? null : tsValue.value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        store.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        clearExpired();
        return unmodifiableSet(store.keySet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        clearExpired();
        return store.values().stream().map(TimestampedValue::getValue).toList();
    }

    @NotNull
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        clearExpired();
        return store.entrySet().stream()
                .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue().getValue()))
                .collect(Collectors.toSet());
    }

    private void clearExpired() {
        for (K k : store.keySet()) {
            this.get(k);
        }
    }

    private class TimestampedValue {
        private final V value;
        private final Instant insertedAt;

        public TimestampedValue(V value) {
            this.value = value;
            this.insertedAt = Instant.now();
        }

        public boolean expired() {
            return Instant.now().isAfter(insertedAt.plus(ttl));
        }

        public V getValue() {
            return value;
        }

    }
}
