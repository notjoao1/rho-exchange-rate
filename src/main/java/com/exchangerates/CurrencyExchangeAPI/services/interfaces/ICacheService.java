package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import java.time.Duration;
import java.util.Optional;

public interface ICacheService<V> {
    /**
     * Retrieves a value from the cache associated to the given key.
     *
     * @param key The key to search for in the cache.
     * @return An Optional with the value associated to the given key if present, otherwise, an empty optional.
     */
    Optional<V> get(String key);

    /**
     * Stores a value in the cache with the specified key and a TTL duration.
     *
     * @param key   The key to store the value under
     * @param value The value to store
     * @param ttl   Duration after which the cached entry should expire.
     */
    void set(String key, V value, Duration ttl);

    /**
     * Stores a value in the cache with the specified key indefinitely.
     *
     * @param key   The key to store the value under
     * @param value The value to store
     */
    void set(String key, V value);
}
