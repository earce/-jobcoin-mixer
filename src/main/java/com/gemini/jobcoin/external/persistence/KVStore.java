package com.gemini.jobcoin.external.persistence;

public interface KVStore<K,V> {

    V put(final K key, V value);

    V get(final K key);

    boolean containsKey(final K key);
}
