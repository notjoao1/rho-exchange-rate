package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

public interface IRateLimitService {
    /**
     * Checks whether the rate limit for the given API key has been exceeded
     * @param apiKey The API key to check the rate limit for
     * @return True if the rate limit has been exceeded, false otherwise
     */
    public boolean isRateLimitExceeded(String apiKey);

    /**
     * Increments the request count for the given API key
     * @param apiKey The API key to increment the request count for
     */
    public void incrementRequestCount(String apiKey);
}
