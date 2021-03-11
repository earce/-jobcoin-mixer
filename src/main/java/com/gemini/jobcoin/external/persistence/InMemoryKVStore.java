package com.gemini.jobcoin.external.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class is a wrapper around a ConcurrentHashMap but is meant to
 * represent what could be an RMDS, NoSQL database or even something
 * more performant like Redis. The idea here being that this data
 * would be persistent + queryable.
 *
 * For the purposes of this project I have decided to keep this
 * part of the implementation simple.
 */
public class InMemoryKVStore<K,V> implements KVStore<K,V> {

    private final Map<K, V> map = new ConcurrentHashMap<>();

    @Override
    public V put(final K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V get(final K key) {
        return map.get(key);
    }

    @Override
    public boolean containsKey(final K key) {
        return map.containsKey(key);
    }
}
