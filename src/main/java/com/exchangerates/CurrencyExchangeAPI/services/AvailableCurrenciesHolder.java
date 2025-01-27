package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyAPIClient;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Holds available currencies in an immutable, lazy loaded and thread-safe way.
 * Double-check pattern for lazy loading: https://en.wikipedia.org/wiki/Double-checked_locking
 */
@Service
public class AvailableCurrenciesHolder {
    private final ICacheService<Set<String>> cacheService;
    private final ICurrencyAPIClient currencyAPIClient;
    private static final String REDIS_KEY = "available:currencies";

    private volatile Set<String> availableCurrencies; // lazy loaded
    // cache it for a long time (2 day)
    private static long AVAILABLE_CURRENCIES_TTL = 60 * 60 * 24;

    @Autowired
    private AvailableCurrenciesHolder(
            ICacheService<Set<String>> cacheService, ICurrencyAPIClient currencyAPIClient) {
        this.cacheService = cacheService;
        this.currencyAPIClient = currencyAPIClient;
    }

    // lazy loaded currencies - first check in-memory, then check in cache,
    // then fetch external API
    public Set<String> getAvailableCurrencies() {
        if (availableCurrencies != null) {
            return availableCurrencies;
        }

        // why isn't the whole function synchronized? the overhead isn't worth it,
        // since after lazy loading, the synchronized code doesn't run very often
        synchronized (this) {
            if (availableCurrencies != null) {
                return availableCurrencies;
            }

            // check cache
            var cachedCurrencies = getCurrenciesFromCache();
            if (cachedCurrencies.isPresent()) {
                availableCurrencies = Collections.unmodifiableSet(cachedCurrencies.get());
                return availableCurrencies;
            }

            // last resort - fetch from external API
            var externalAvailableCurrencies =
                    currencyAPIClient.fetchAvailableCurrencies().getCurrencies().keySet();

            availableCurrencies = Collections.unmodifiableSet(externalAvailableCurrencies);
            cacheService.set(
                    REDIS_KEY, availableCurrencies, Duration.ofSeconds(AVAILABLE_CURRENCIES_TTL));

            return availableCurrencies;
        }
    }

    private Optional<Set<String>> getCurrenciesFromCache() {
        return cacheService.get(REDIS_KEY);
    }
}
