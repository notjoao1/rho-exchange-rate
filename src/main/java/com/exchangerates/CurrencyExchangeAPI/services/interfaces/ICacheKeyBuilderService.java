package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import java.util.Optional;

public interface ICacheKeyBuilderService {
    /**
     * Builds a cache key based on the given base currency and target currency.
     *
     * @param baseCurrency   The base currency to build the cache key with.
     * @param targetCurrency The target currency to build the cache key with.
     * @return The cache key built from the given base currency and target currency.
     */
    String buildCacheKey(String baseCurrency, Optional<String> targetCurrency);
}
